package silverbell.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import silverbell.common.Message;
import silverbell.common.MessageType;

/** 테스트 안에서 작은 가짜 서버를 열어 클라이언트 연결을 확인한다. */
@Timeout(10)
class ServerConnectionTest {

    private ServerSocket fakeServer;
    private final RecordingListener listener = new RecordingListener();

    @BeforeEach
    void openFakeServer() throws IOException {
        fakeServer = new ServerSocket(0);
    }

    @AfterEach
    void closeFakeServer() throws IOException {
        fakeServer.close();
    }

    @Test
    @DisplayName("서버가 없으면 connect가 IOException을 던진다 (창은 꺼지지 않고 화면이 안내한다)")
    void noServer() throws IOException {
        // given
        int port = fakeServer.getLocalPort();
        fakeServer.close();

        // when · then
        assertThrows(IOException.class, () -> ServerConnection.connect("127.0.0.1", port, listener));
    }

    @Test
    @DisplayName("보낸 메시지가 서버에 그대로 가고, 받은 메시지는 엉뚱한 줄을 건너뛰고 listener로 온다")
    void sendAndReceive() throws Exception {
        // given
        try (ServerConnection connection = ServerConnection.connect("127.0.0.1", fakeServer.getLocalPort(), listener);
             Socket server = fakeServer.accept()) {
            DataInputStream fromClient = new DataInputStream(server.getInputStream());
            DataOutputStream toClient = new DataOutputStream(server.getOutputStream());

            // when
            connection.send(Message.of(MessageType.JOIN, "이건"));
            toClient.writeUTF("");
            toClient.writeUTF("HELLO|world");
            toClient.writeUTF("CHAT|주성;안녕 ;)");
            toClient.flush();

            // then
            assertEquals("JOIN|이건", fromClient.readUTF());
            assertEquals(Message.parse("CHAT|주성;안녕 ;)"), listener.messages.poll(2, TimeUnit.SECONDS));
            assertTrue(listener.messages.isEmpty());
        }
    }

    @Test
    @DisplayName("서버가 끊으면 onDisconnected가 한 번만 불리고, 그 뒤 send는 false다")
    void serverClosed() throws Exception {
        // given
        ServerConnection connection = ServerConnection.connect("127.0.0.1", fakeServer.getLocalPort(), listener);
        Socket server = fakeServer.accept();

        // when
        server.close();
        waitUntil(() -> listener.disconnects.get() == 1);
        boolean sent = connection.send(Message.of(MessageType.BUZZ));

        // then
        assertFalse(sent);
        Thread.sleep(200);
        assertEquals(1, listener.disconnects.get());
    }

    @Test
    @DisplayName("직접 close하면 onDisconnected를 부르지 않는다")
    void closeByUser() throws Exception {
        // given
        ServerConnection connection = ServerConnection.connect("127.0.0.1", fakeServer.getLocalPort(), listener);
        try (Socket ignored = fakeServer.accept()) {

            // when
            connection.close();
            Thread.sleep(200);

            // then
            assertEquals(0, listener.disconnects.get());
            assertFalse(connection.send(Message.of(MessageType.BUZZ)));
        }
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2_000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.getAsBoolean());
    }

    private static final class RecordingListener implements MessageListener {
        final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();
        final AtomicInteger disconnects = new AtomicInteger();

        @Override
        public void onMessage(Message message) {
            messages.add(message);
        }

        @Override
        public void onDisconnected() {
            disconnects.incrementAndGet();
        }
    }
}
