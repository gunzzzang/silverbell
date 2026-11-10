package silverbell.server.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import silverbell.common.Message;
import silverbell.common.MessageType;
import silverbell.server.game.ConnectionListener;
import silverbell.server.handler.MessageDispatcher;

/**
 * 진짜 소켓으로 서버를 띄워 확인한다. 빈 포트(0)를 쓰므로 실행 중인 서버와 겹치지 않는다.
 * 클라이언트는 수업 예제처럼 {@code writeUTF}로 아무 문자열이나 보낼 수 있는 {@link RawClient}를 쓴다.
 */
@Timeout(10)
class SocketServerTest {

    private final ClientRegistry registry = new ClientRegistry();
    private final MessageDispatcher dispatcher = new MessageDispatcher();
    private final RecordingListener listener = new RecordingListener();
    private final BlockingQueue<String> chats = new LinkedBlockingQueue<>();
    private final List<RawClient> clients = new ArrayList<>();
    private SocketServer server;

    @BeforeEach
    void startServer() throws IOException {
        dispatcher.register(MessageType.CHAT, (sender, message) -> chats.add(sender + ":" + message.body()));
        server = new SocketServer(0, dispatcher, registry, listener);
        server.start();
    }

    @AfterEach
    void stopServer() {
        clients.forEach(RawClient::closeQuietly);
        server.close();
    }

    @Test
    @DisplayName("JOIN하면 JOIN_OK를 받고, 그 뒤 메시지는 서버가 확인한 닉네임과 함께 핸들러로 간다")
    void joinThenDispatch() throws Exception {
        // given
        RawClient client = connect();

        // when
        client.send("JOIN|이건");
        String reply = client.receive();
        client.send("CHAT|안녕;하세요");

        // then
        assertEquals("JOIN_OK|", reply);
        assertEquals("이건", listener.joined.poll(2, TimeUnit.SECONDS));
        assertEquals("이건:안녕;하세요", chats.poll(2, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("입장 전에 보낸 메시지는 핸들러로 가지 않는다")
    void ignoreBeforeJoin() throws Exception {
        // given
        RawClient client = connect();

        // when
        client.send("CHAT|몰래");
        client.join("이건");
        client.send("CHAT|입장 후");

        // then
        assertEquals("이건:입장 후", chats.poll(2, TimeUnit.SECONDS));
        assertTrue(chats.isEmpty());
    }

    @Test
    @DisplayName("같은 닉네임으로 들어오면 새 연결만 JOIN_FAIL을 받고 기존 사람은 아무것도 받지 않는다")
    void duplicateNickname() throws Exception {
        // given
        RawClient first = connect();
        first.join("이건");
        RawClient second = connect();

        // when
        second.send("JOIN|이건");

        // then
        assertEquals("JOIN_FAIL|" + ClientHandler.DUPLICATE_NICKNAME, second.receive());
        assertNull(first.receiveWithin(300));
        assertEquals(1, registry.size());
    }

    @Test
    @DisplayName("게임이 거절하면 사유와 함께 JOIN_FAIL을 받고 닉네임은 다시 쓸 수 있다")
    void rejectedByGame() throws Exception {
        // given
        listener.rejectReason = "인원 초과";
        RawClient client = connect();

        // when
        client.send("JOIN|이건");
        String reply = client.receive();
        listener.rejectReason = null;
        client.send("JOIN|이건");

        // then
        assertEquals("JOIN_FAIL|인원 초과", reply);
        assertEquals("JOIN_OK|", client.receive());
    }

    @Test
    @DisplayName("빈 줄, |가 없는 줄, 모르는 타입, 서버 전용 메시지를 받아도 무시하고 연결은 유지된다")
    void ignoreBrokenLines() throws Exception {
        // given
        RawClient client = connect();
        client.join("이건");

        // when
        client.send("");
        client.send("아무말");
        client.send("HELLO|world");
        client.send("SCORE|이건;999");
        client.send("CHAT|아직 연결됨");

        // then
        assertEquals("이건:아직 연결됨", chats.poll(2, TimeUnit.SECONDS));
        assertTrue(chats.isEmpty());
    }

    @Test
    @DisplayName("한 클라이언트가 강제 종료돼도 퇴장이 한 번 알려지고 다른 사람은 계속 받는다")
    void survivesAbruptDisconnect() throws Exception {
        // given
        RawClient leaving = connect();
        leaving.join("주성");
        RawClient staying = connect();
        staying.join("이건");

        // when
        leaving.closeQuietly();
        String left = listener.left.poll(2, TimeUnit.SECONDS);
        registry.broadcast(Message.of(MessageType.SYSTEM, "남은 사람에게"));

        // then
        assertEquals("주성", left);
        assertEquals("SYSTEM|남은 사람에게", staying.receive());
        assertEquals(1, registry.size());
        assertNull(listener.left.poll(300, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("여러 스레드가 동시에 보내도 메시지가 섞이거나 빠지지 않는다")
    void concurrentSendsDoNotMix() throws Exception {
        // given
        RawClient a = connect();
        a.join("이건");
        RawClient b = connect();
        b.join("주성");
        int threads = 10;
        int perThread = 100;
        CountDownLatch ready = new CountDownLatch(1);
        List<Thread> senders = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int id = t;
            Thread sender = new Thread(() -> {
                awaitQuietly(ready);
                for (int i = 0; i < perThread; i++) {
                    registry.broadcast(Message.of(MessageType.CHAT, "t" + id, "메시지 " + i + " ;|섞이면 안 됨"));
                }
            });
            senders.add(sender);
            sender.start();
        }

        // when
        ready.countDown();
        Set<String> receivedByA = receiveAll(a, threads * perThread);
        Set<String> receivedByB = receiveAll(b, threads * perThread);

        // then
        Set<String> expected = new HashSet<>();
        for (int t = 0; t < threads; t++) {
            for (int i = 0; i < perThread; i++) {
                expected.add("CHAT|t" + t + ";메시지 " + i + " ;|섞이면 안 됨");
            }
        }
        assertEquals(expected, receivedByA);
        assertEquals(expected, receivedByB);
        for (Thread sender : senders) {
            sender.join();
        }
    }

    @Test
    @DisplayName("64KB가 넘는 메시지는 버리고 연결은 유지된다")
    void tooLongMessageIsDropped() throws Exception {
        // given
        RawClient client = connect();
        client.join("이건");

        // when
        registry.sendTo("이건", Message.of(MessageType.SYSTEM, "가".repeat(30_000)));
        registry.sendTo("이건", Message.of(MessageType.SYSTEM, "짧은 메시지"));

        // then
        assertEquals("SYSTEM|짧은 메시지", client.receive());
    }

    @Test
    @DisplayName("이미 쓰고 있는 포트로 서버를 만들면 예외가 난다")
    void portInUse() {
        assertThrows(IOException.class, () -> new SocketServer(server.port(), dispatcher, registry, listener));
    }

    // ── 도우미 ─────────────────────────────────────

    private RawClient connect() throws IOException {
        RawClient client = new RawClient(server.port());
        clients.add(client);
        return client;
    }

    private static Set<String> receiveAll(RawClient client, int count) throws IOException {
        Set<String> received = new HashSet<>();
        for (int i = 0; i < count; i++) {
            received.add(client.receive());
        }
        return received;
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 수업 예제 클라이언트처럼 writeUTF·readUTF로 아무 문자열이나 주고받는다. */
    private static final class RawClient {
        private final Socket socket;
        private final DataInputStream in;
        private final DataOutputStream out;

        RawClient(int port) throws IOException {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(3_000);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
        }

        void send(String line) throws IOException {
            out.writeUTF(line);
            out.flush();
        }

        String receive() throws IOException {
            return in.readUTF();
        }

        /** 이 시간 안에 받은 게 없으면 {@code null}. */
        String receiveWithin(int millis) throws IOException {
            socket.setSoTimeout(millis);
            try {
                return in.readUTF();
            } catch (SocketTimeoutException e) {
                return null;
            } finally {
                socket.setSoTimeout(3_000);
            }
        }

        void join(String nickname) throws IOException {
            send("JOIN|" + nickname);
            assertEquals("JOIN_OK|", receive());
        }

        void closeQuietly() {
            try {
                socket.close();
            } catch (IOException ignored) {
                // 이미 닫혔다
            }
        }
    }

    /** 게임 쪽 대신 입장·퇴장을 기록한다. {@code rejectReason}을 넣으면 그 사유로 거절한다. */
    private static final class RecordingListener implements ConnectionListener {
        final BlockingQueue<String> joined = new LinkedBlockingQueue<>();
        final BlockingQueue<String> left = new LinkedBlockingQueue<>();
        volatile String rejectReason;

        @Override
        public String tryJoin(String nickname) {
            return rejectReason;
        }

        @Override
        public void joined(String nickname) {
            joined.add(nickname);
        }

        @Override
        public void left(String nickname) {
            left.add(nickname);
        }
    }
}
