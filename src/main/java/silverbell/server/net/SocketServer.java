package silverbell.server.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import silverbell.server.game.ConnectionListener;
import silverbell.server.handler.MessageDispatcher;

/**
 * 포트를 열고 접속을 받아 클라이언트마다 {@link ClientHandler} 스레드를 띄운다. 수업 예제의 {@code AcceptServer}를 옮겼다.
 *
 * <p>예제와 다른 점:
 * <ul>
 *   <li>포트를 생성자에서 연다. 이미 쓰는 포트면 여기서 예외가 나서 서버가 「켜진 척」하지 않는다
 *       (예제는 실패해도 접속 대기 스레드를 띄워 {@code NullPointerException}이 났다).</li>
 *   <li>접속 받는 스레드는 받기만 하고 첫 메시지를 기다리지 않는다.</li>
 *   <li>{@link #close}로 끌 수 있다. JUnit 테스트에서 서버를 켰다 끈다.</li>
 * </ul>
 */
public final class SocketServer extends Thread implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final MessageDispatcher dispatcher;
    private final ClientRegistry registry;
    private final ConnectionListener listener;
    /** 입장 전까지 포함한 모든 연결. 예제의 {@code UserVec}처럼 각 {@link ClientHandler}가 끊길 때 스스로 빠진다. */
    private final Vector<ClientHandler> connections = new Vector<>();

    /**
     * 포트를 연다. 0이면 빈 포트를 아무거나 쓴다(테스트용, {@link #port()}로 확인).
     *
     * @throws IOException 포트를 열 수 없을 때 (이미 서버가 켜져 있는 등)
     */
    public SocketServer(int port, MessageDispatcher dispatcher, ClientRegistry registry, ConnectionListener listener)
            throws IOException {
        this.serverSocket = new ServerSocket(port); // 서버 소켓 생성. 실패하면 여기서 예외
        this.dispatcher = dispatcher;
        this.registry = registry;
        this.listener = listener;
    }

    /** 실제로 열린 포트. */
    public int port() {
        return serverSocket.getLocalPort();
    }

    /** 접속 받기를 멈추고 모든 연결을 닫는다. */
    @Override
    public void close() {
        try {
            serverSocket.close(); // accept()에서 기다리던 이 스레드가 예외로 깨어나 끝난다
        } catch (IOException e) {
            // 이미 닫혔다
        }
        synchronized (connections) { // 도는 중에 끊긴 연결이 목록에서 빠지지 않게
            for (int i = 0; i < connections.size(); i++) {
                connections.get(i).close();
            }
        }
    }

    /** 사용자 접속을 계속 받는다. {@code start()}로 이 스레드를 띄우면 실행된다. */
    @Override
    public void run() {
        ServerLog.info("서버 시작 · 포트 " + port());
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept(); // 접속이 올 때까지 대기. 예제와 달리 지역 변수
                startHandler(socket);
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    break;
                }
                // 한 사람 접속이 실패해도 서버는 계속 받는다
                ServerLog.info("접속 받기 실패: " + e.getMessage());
            }
        }
        ServerLog.info("서버 종료");
    }

    private void startHandler(Socket socket) throws IOException {
        try {
            socket.setTcpNoDelay(true); // 벨처럼 작은 메시지를 모으지 않고 바로 보낸다
            ClientHandler handler = new ClientHandler(socket, dispatcher, registry, listener, connections);
            connections.add(handler); // 연결 목록에 추가
            handler.start();          // 사용자 한 명당 스레드 하나 실행
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }
}
