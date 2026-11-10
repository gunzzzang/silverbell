package silverbell.server.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.Socket;

import silverbell.common.Message;
import silverbell.common.MessageType;
import silverbell.server.game.ConnectionListener;
import silverbell.server.handler.MessageDispatcher;

/**
 * 접속한 클라이언트 한 명을 맡는다. 수업 예제의 {@code UserService} 스레드를 옮겼다.
 * 받은 메시지를 {@link MessageDispatcher}로 넘기기만 하고 게임 규칙은 여기에 쓰지 않는다.
 *
 * <p>예제와 다른 점:
 * <ul>
 *   <li>로그인({@code JOIN})을 생성자가 아니라 이 연결의 스레드에서 받는다. 예제는 접속 받는 스레드가 첫 메시지를
 *       기다리는 동안 다른 사람이 접속하지 못했다.</li>
 *   <li>입장 전에는 {@code JOIN}만 받는다. 그래서 핸들러가 받는 보낸 사람은 항상 서버가 확인한 닉네임이다.</li>
 *   <li>엉뚱한 메시지나 핸들러 오류가 나도 그 메시지만 버리고 연결은 유지한다. 예제는 {@code args[1]}이 없으면
 *       예외로 스레드가 죽고 퇴장 처리도 안 됐다.</li>
 *   <li>{@link #send}를 {@code synchronized}로 막는다. 여러 스레드(벨, 타이머, 채팅)가 한 사람에게 동시에 보내도
 *       메시지가 섞이지 않는다.</li>
 * </ul>
 */
public final class ClientHandler implements Runnable {

    /** {@code JOIN_FAIL} 사유. 나머지 사유는 {@link ConnectionListener#tryJoin}이 정한다 (#5). */
    static final String DUPLICATE_NICKNAME = "닉네임 중복";
    static final String SERVER_ERROR = "서버 오류";

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final MessageDispatcher dispatcher;
    private final ClientRegistry registry;
    private final ConnectionListener listener;
    private final String address;

    /** 입장하기 전에는 {@code null}. 받는 스레드만 쓰고, 로그·보내기에서 읽기만 한다. */
    private volatile String nickname;

    ClientHandler(Socket socket, MessageDispatcher dispatcher, ClientRegistry registry, ConnectionListener listener)
            throws IOException {
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.dispatcher = dispatcher;
        this.registry = registry;
        this.listener = listener;
        this.address = socket.getRemoteSocketAddress().toString();
    }

    /** 연결이 끊길 때까지 메시지를 받는다. 끊기면 목록에서 빼고 게임에 퇴장을 알린다. */
    @Override
    public void run() {
        ServerLog.info("접속 " + address);
        try {
            while (true) {
                String line = in.readUTF();
                ServerLog.info("받음 " + who() + ": " + line);
                handleLine(line);
            }
        } catch (EOFException e) {
            // 상대가 소켓을 정상적으로 닫았다
        } catch (IOException e) {
            ServerLog.info("연결 끊김 " + who() + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    /**
     * 이 연결에 한 메시지를 보낸다. 여러 스레드가 동시에 불러도 하나씩 보낸다.
     *
     * @return 보냈으면 {@code true}. 너무 길어서 못 보낸 메시지는 버리고 연결은 유지한다.
     *         연결이 끊겨 실패하면 소켓을 닫고 {@code false} (정리는 받는 스레드가 한다).
     */
    public synchronized boolean send(Message message) {
        if (socket.isClosed()) {
            return false;
        }
        String line = message.toLine();
        try {
            out.writeUTF(line);
            out.flush();
            ServerLog.info("보냄 " + who() + ": " + line);
            return true;
        } catch (UTFDataFormatException e) {
            // writeUTF는 64KB까지만 보낸다. 길이를 먼저 재고 던지므로 스트림은 깨지지 않는다
            ServerLog.info("안 보냄 " + who() + ": 너무 긴 메시지 (" + line.length() + "자)");
            return false;
        } catch (IOException e) {
            close();
            return false;
        }
    }

    /** 소켓을 닫는다. 받는 스레드의 {@code readUTF}가 깨어나 {@link #run}의 정리가 돈다. */
    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 이미 닫혔다
        }
    }

    private void handleLine(String line) {
        Message message;
        try {
            message = Message.parse(line);
        } catch (IllegalArgumentException e) {
            ServerLog.info("무시 " + who() + ": 형식이 맞지 않음");
            return;
        }

        if (nickname == null) {
            handleBeforeJoin(message);
            return;
        }
        if (message.type() == MessageType.JOIN) {
            ServerLog.info("무시 " + who() + ": 이미 입장함");
            return;
        }
        try {
            if (!dispatcher.dispatch(nickname, message)) {
                ServerLog.info("무시 " + who() + ": 서버가 처리하지 않는 종류 " + message.type());
            }
        } catch (RuntimeException e) {
            // 핸들러 하나의 버그로 이 사람의 연결까지 끊기지 않게 한다
            ServerLog.info("핸들러 오류 " + message.type() + ": " + e);
            e.printStackTrace();
        }
    }

    private void handleBeforeJoin(Message message) {
        if (message.type() != MessageType.JOIN) {
            ServerLog.info("무시 " + who() + ": 입장 전 " + message.type());
            return;
        }
        String requested = message.body().strip();
        String reason = registry.reserve(requested, this) ? askJoin(requested) : DUPLICATE_NICKNAME;
        if (reason != null) {
            registry.release(requested, this);
            send(Message.of(MessageType.JOIN_FAIL, reason));
            return;
        }
        nickname = requested;
        send(Message.of(MessageType.JOIN_OK));
        registry.activate(requested, this); // JOIN_OK를 먼저 받은 뒤부터 전체 메시지를 받는다
        try {
            listener.joined(requested);
        } catch (RuntimeException e) {
            ServerLog.info("입장 처리 오류 " + requested + ": " + e);
            e.printStackTrace();
        }
    }

    /** 게임에 입장을 묻는다. 게임 쪽 버그로 예외가 나면 잡아 둔 닉네임이 남지 않게 실패로 돌린다. */
    private String askJoin(String requested) {
        try {
            return listener.tryJoin(requested);
        } catch (RuntimeException e) {
            ServerLog.info("입장 검사 오류 " + requested + ": " + e);
            e.printStackTrace();
            return SERVER_ERROR;
        }
    }

    private void cleanup() {
        close();
        String name = nickname;
        if (name == null) {
            ServerLog.info("연결 종료 " + address + " (입장 전)");
            return;
        }
        registry.release(name, this);
        ServerLog.info("퇴장 " + name);
        try {
            listener.left(name);
        } catch (RuntimeException e) {
            ServerLog.info("퇴장 처리 오류 " + name + ": " + e);
            e.printStackTrace();
        }
    }

    private String who() {
        String name = nickname;
        return name != null ? name : address;
    }
}
