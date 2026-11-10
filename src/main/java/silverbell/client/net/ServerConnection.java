package silverbell.client.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean closed = new AtomicBoolean();

    private ServerConnection(Socket socket, MessageListener listener) throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
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
            socket.connect(new InetSocketAddress(host, port), GameConfig.CONNECT_TIMEOUT_MS);
            socket.setTcpNoDelay(true); // 벨을 바로 보낸다
            ServerConnection connection = new ServerConnection(socket, listener);
            Thread reader = new Thread(connection::readLoop, "server-reader");
            reader.setDaemon(true); // 창을 닫으면 이 스레드 때문에 프로그램이 남지 않게
            reader.start();
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
    public synchronized boolean send(Message message) {
        if (closed.get()) {
            return false;
        }
        try {
            out.writeUTF(message.toLine());
            out.flush();
            return true;
        } catch (UTFDataFormatException e) {
            return false; // 64KB가 넘는 메시지는 보내지 않는다. 연결은 그대로다
        } catch (IOException e) {
            closeSocket(); // 받는 스레드가 깨어나 onDisconnected를 부른다
            return false;
        }
    }

    /** 사용자가 나갈 때 연결을 닫는다. 이때는 {@code onDisconnected}를 부르지 않는다. */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            closeSocket();
        }
    }

    private void readLoop() {
        try {
            while (true) {
                String line = in.readUTF();
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
            if (closed.compareAndSet(false, true)) {
                closeSocket();
                listener.onDisconnected();
            }
        }
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 이미 닫혔다
        }
    }
}
