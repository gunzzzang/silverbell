package silverbell.server.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
public final class SocketServer implements AutoCloseable {

    private final ServerSocket serverSocket;
    private final MessageDispatcher dispatcher;
    private final ClientRegistry registry;
    private final ConnectionListener listener;
    private final Set<ClientHandler> connections = ConcurrentHashMap.newKeySet();
    private final AtomicInteger connectionCount = new AtomicInteger();

    /**
     * 포트를 연다. 0이면 빈 포트를 아무거나 쓴다(테스트용, {@link #port()}로 확인).
     *
     * @throws IOException 포트를 열 수 없을 때 (이미 서버가 켜져 있는 등)
     */
    public SocketServer(int port, MessageDispatcher dispatcher, ClientRegistry registry, ConnectionListener listener)
            throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.dispatcher = dispatcher;
        this.registry = registry;
        this.listener = listener;
    }

    /** 접속 받는 스레드를 띄운다. */
    public void start() {
        new Thread(this::acceptLoop, "accept").start();
    }

    /** 실제로 열린 포트. */
    public int port() {
        return serverSocket.getLocalPort();
    }

    /** 접속 받기를 멈추고 모든 연결을 닫는다. */
    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // 이미 닫혔다
        }
        connections.forEach(ClientHandler::close);
    }

    private void acceptLoop() {
        ServerLog.info("서버 시작 · 포트 " + port());
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
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
            ClientHandler handler = new ClientHandler(socket, dispatcher, registry, listener);
            connections.add(handler);
            Thread thread = new Thread(() -> {
                try {
                    handler.run();
                } finally {
                    connections.remove(handler);
                }
            }, "client-" + connectionCount.incrementAndGet());
            thread.start();
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }
}
