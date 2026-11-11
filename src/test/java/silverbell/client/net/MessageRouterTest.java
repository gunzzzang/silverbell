package silverbell.client.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.common.Message;
import silverbell.common.MessageType;

class MessageRouterTest {

    /** 화면 스레드 대신 바로 부른다. */
    private final MessageRouter router = new MessageRouter(false);

    @Test
    @DisplayName("등록한 종류의 메시지만 그 곳이 받는다")
    void routeByType() {
        // given
        List<String> chat = new ArrayList<>();
        router.on(MessageType.CHAT, message -> chat.add(message.body()));

        // when
        router.onMessage(Message.parse("CHAT|이건;안녕"));
        router.onMessage(Message.parse("BUZZ|"));

        // then
        assertEquals(List.of("이건;안녕"), chat);
    }

    @Test
    @DisplayName("한 종류에 두 곳이 등록하면 둘 다 받는다")
    void manyHandlersForOneType() {
        // given
        List<String> received = new ArrayList<>();
        router.on(MessageType.SCORE, message -> received.add("점수판"));
        router.on(MessageType.SCORE, message -> received.add("결과 화면"));

        // when
        router.onMessage(Message.parse("SCORE|이건;3"));

        // then
        assertEquals(List.of("점수판", "결과 화면"), received);
    }

    @Test
    @DisplayName("연결이 끊기면 등록한 끊김 처리가 불린다")
    void disconnected() {
        // given
        List<String> called = new ArrayList<>();
        router.whenDisconnected(() -> called.add("타이틀로"));

        // when
        router.onDisconnected();

        // then
        assertEquals(List.of("타이틀로"), called);
    }
}
