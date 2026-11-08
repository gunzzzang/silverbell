package silverbell.common;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    @DisplayName("보낸 메시지를 다시 읽으면 같은 값이 나온다")
    void roundTrip() {
        // given
        Message sent = Message.of(MessageType.CHAT, "이건", "안녕");

        // when
        Message received = Message.parse(sent.toLine());

        // then
        assertEquals("CHAT|이건;안녕", sent.toLine());
        assertEquals(sent, received);
        assertArrayEquals(new String[] {"이건", "안녕"}, received.values(2));
    }

    @Test
    @DisplayName("채팅 글에 | 나 ; 가 있어도 마지막 값으로 그대로 받는다")
    void separatorsInsideLastValue() {
        // given
        String chat = "ㅋㅋ|진짜;대박";

        // when
        Message received = Message.parse(Message.of(MessageType.CHAT, "주성", chat).toLine());

        // then
        assertEquals(MessageType.CHAT, received.type());
        assertArrayEquals(new String[] {"주성", chat}, received.values(2));
    }

    @Test
    @DisplayName("값이 없는 메시지는 내용이 빈 문자열이다")
    void noValues() {
        // when
        Message received = Message.parse("BUZZ");

        // then
        assertEquals(MessageType.BUZZ, received.type());
        assertEquals("", received.body());
    }

    @Test
    @DisplayName("목록 메시지는 개수 제한 없이 전부 자른다")
    void listValues() {
        // when
        Message received = Message.parse("QUEUE|주성;이건;민수");

        // then
        assertArrayEquals(new String[] {"주성", "이건", "민수"}, received.values(0));
    }

    @Test
    @DisplayName("모르는 타입을 받으면 예외가 나서 받는 쪽이 그 줄을 무시할 수 있다")
    void unknownType() {
        assertThrows(IllegalArgumentException.class, () -> Message.parse("HACK|1000"));
    }

    @Test
    @DisplayName("마지막이 아닌 값에 ; 를 넣으면 보내기 전에 막는다")
    void separatorInMiddleValue() {
        assertThrows(IllegalArgumentException.class,
                () -> Message.of(MessageType.WHISPER, "주;성", "안녕"));
    }

    @Test
    @DisplayName("값에 줄바꿈을 넣으면 보내기 전에 막는다")
    void newlineInValue() {
        assertThrows(IllegalArgumentException.class,
                () -> Message.of(MessageType.CHAT, "이건", "첫 줄\n둘째 줄"));
    }
}
