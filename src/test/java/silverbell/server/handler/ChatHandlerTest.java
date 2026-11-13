package silverbell.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.common.Message;
import silverbell.common.MessageType;
import silverbell.server.game.Broadcaster;

class ChatHandlerTest {

    private DummyBroadcaster broadcaster;
    private ChatHandler chatHandler;

    static class DummyBroadcaster implements Broadcaster {
        final List<Message> broadcastedMessages = new ArrayList<>();

        @Override
        public void broadcast(Message message) {
            broadcastedMessages.add(message);
        }

        @Override
        public void sendTo(String nickname, Message message) {
        }
    }

    @BeforeEach
    void setUp() {
        broadcaster = new DummyBroadcaster();
        chatHandler = new ChatHandler(broadcaster);
    }

    @Test
    @DisplayName("정상 채팅 시 서버가 확인한 sender 닉네임을 붙여 전체에 전달한다")
    void testBroadcastChatWithServerSender() {
        chatHandler.handle("주성", Message.of(MessageType.CHAT, "안녕하세요"));

        assertEquals(1, broadcaster.broadcastedMessages.size());
        Message sent = broadcaster.broadcastedMessages.get(0);
        assertEquals(MessageType.CHAT, sent.type());
        assertEquals("주성;안녕하세요", sent.body());
        assertEquals("주성", sent.values(2)[0]);
        assertEquals("안녕하세요", sent.values(2)[1]);
    }

    @Test
    @DisplayName("채팅 내용에 파이프(|)와 세미콜론(;)이 포함되어도 깨지지 않는다")
    void testSpecialCharactersPreserved() {
        String specialText = "ㅋㅋ|;ㅋ;테스트|완료";
        chatHandler.handle("이건", Message.of(MessageType.CHAT, specialText));

        assertEquals(1, broadcaster.broadcastedMessages.size());
        String[] parsed = broadcaster.broadcastedMessages.get(0).values(2);
        assertEquals("이건", parsed[0]);
        assertEquals(specialText, parsed[1]);
    }

    @Test
    @DisplayName("빈 메시지나 공백만 있는 메시지는 전송하지 않고 무시한다")
    void testBlankMessageIgnored() {
        chatHandler.handle("주성", Message.of(MessageType.CHAT, ""));
        chatHandler.handle("주성", Message.of(MessageType.CHAT, "   \t  "));

        assertTrue(broadcaster.broadcastedMessages.isEmpty());
    }
}