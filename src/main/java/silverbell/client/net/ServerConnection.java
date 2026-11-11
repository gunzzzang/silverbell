package silverbell.client.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.InetSocketAddress;
import java.net.Socket;

import silverbell.common.GameConfig;
import silverbell.common.Message;

/**
 * 서버와의 연결 하나. 수업 예제 {@code JavaChatClientView}의 소켓·{@code ListenNetwork}·{@code SendMessage}를 옮겼다.
 * 화면 코드는 여기에 쓰지 않고, 받은 메시지는 {@link MessageListener}에게 알리기만 한다.
 *
 * <p>예제와 다른 점:
 * <ul>
 *   <li>접속을 화면 생성자에서 하지 않는다. {@link #connect}는 화면 스레드가 아닌 곳에서 부른다(#6).
 *       예제는 서버가 없으면 접속을 기다리는 동안 창이 멈췄다.</li>
 *   <li>서버가 없으면 예외를 던져 부른 쪽(타이틀 화면)이 안내하게 한다. 창은 꺼지지 않는다(N5).</li>
 *   <li>모르는 메시지는 건너뛰고 계속 받는다.</li>
 *   <li>끊김은 {@link MessageListener#onDisconnected}로 딱 한 번 알린다(G2). 직접 {@link #close}하면 알리지 않는다.</li>
 * </ul>
 */
public final class ServerConnection implements AutoCloseable {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final MessageListener listener;
    /** 닫혔는지. {@link #markClosed}로만 바꾼다. 예제 {@code MessengerMulti}의 {@code running} 플래그와 같은 역할 */
    private volatile boolean closed;

    private ServerConnection(Socket socket, MessageListener listener) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());    // 소켓 입력 스트림
        this.out = new DataOutputStream(socket.getOutputStream()); // 소켓 출력 스트림
        this.listener = listener;
    }

    /**
     * 서버에 접속하고 받는 스레드를 띄운다. 접속하는 동안(최대 {@value GameConfig#CONNECT_TIMEOUT_MS}ms) 멈추므로
     * 화면 스레드(EDT)에서 부르지 않는다.
     *
     * @throws IOException 서버가 없거나 주소가 틀렸을 때
     */
    public static ServerConnection connect(String host, int port, MessageListener listener) throws IOException {
        Socket socket = new Socket();
        try {
            // new Socket(host, port)는 IP가 틀리면 오래 멈춰서, 기다릴 시간을 정해 접속한다(N5)
            socket.connect(new InetSocketAddress(host, port), GameConfig.CONNECT_TIMEOUT_MS);
            socket.setTcpNoDelay(true); // 벨을 모으지 않고 바로 보낸다
            ServerConnection connection = new ServerConnection(socket, listener);
            connection.startListening();
            return connection;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    /**
     * 서버로 한 메시지를 보낸다. 여러 스레드가 불러도 하나씩 보낸다.
     *
     * @return 보냈으면 {@code true}. 끊겨서 못 보내면 {@code false}이고 끊김 알림은 받는 스레드가 한다.
     */
    public boolean send(Message message) {
        synchronized (out) { // 보내기끼리만 줄을 세운다. 닫기는 보내는 중에도 할 수 있게
            if (closed) {
                return false;
            }
            try {
                out.writeUTF(message.toLine()); // 메시지 하나 전송
                out.flush();
                return true;
            } catch (UTFDataFormatException e) {
                return false; // 64KB가 넘는 메시지는 보내지 않는다. 연결은 그대로다
            } catch (IOException e) {
                closeSocket(); // 받는 스레드가 깨어나 onDisconnected를 부른다
                return false;
            }
        }
    }

    /** 사용자가 나갈 때 연결을 닫는다. 이때는 {@code onDisconnected}를 부르지 않는다. */
    @Override
    public void close() {
        if (markClosed()) {
            closeSocket();
        }
    }

    private void startListening() {
        ListenNetwork net = new ListenNetwork();
        net.start();
    }

    /** 처음 닫는 쪽만 {@code true}를 받는다. 사용자의 {@link #close}와 받는 스레드의 끊김 처리가 겹쳐도 한 번만 정리한다. */
    private synchronized boolean markClosed() {
        if (closed) {
            return false;
        }
        closed = true;
        return true;
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            // 이미 닫혔다
        }
    }

    /** 서버 메시지를 받아 listener에게 알린다. 예제 {@code JavaChatClientView.ListenNetwork}와 같다. */
    class ListenNetwork extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    String line = in.readUTF(); // 서버가 보낼 때까지 대기
                    Message message;
                    try {
                        message = Message.parse(line);
                    } catch (IllegalArgumentException e) {
                        continue; // 모르는 메시지는 건너뛴다
                    }
                    try {
                        listener.onMessage(message);
                    } catch (RuntimeException e) {
                        e.printStackTrace(); // 화면 쪽 오류로 받기가 멈추지 않게
                    }
                }
            } catch (IOException e) {
                // 서버가 꺼졌거나 소켓이 닫혔다
            } finally {
                if (markClosed()) { // 사용자가 직접 닫은 게 아니면 끊김을 알린다
                    closeSocket();
                    listener.onDisconnected();
                }
            }
        }
    }
}
