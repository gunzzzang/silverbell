package silverbell.client.ui.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.common.Message;

class QueuePanelTest {

    @Test
    @DisplayName("줄이 비면(QUEUE|) 빈 배열이 된다")
    void emptyQueue() {
        // given
        Message message = Message.parse("QUEUE|");

        // when
        String[] order = QueuePanel.readOrder(message);

        // then
        assertArrayEquals(new String[0], order); // values(0)은 빈 내용에서 [""]를 돌려줘서 따로 막는다
    }

    @Test
    @DisplayName("한 명이면 그 사람만 담긴다")
    void oneInQueue() {
        // given
        Message message = Message.parse("QUEUE|주성");

        // when
        String[] order = QueuePanel.readOrder(message);

        // then
        assertArrayEquals(new String[] {"주성"}, order);
    }

    @Test
    @DisplayName("여러 명이면 서버가 보낸 순서 그대로 담긴다")
    void keepServerOrder() {
        // given
        Message message = Message.parse("QUEUE|주성;이건;민수");

        // when
        String[] order = QueuePanel.readOrder(message);

        // then
        assertArrayEquals(new String[] {"주성", "이건", "민수"}, order);
    }
}
