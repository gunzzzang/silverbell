package silverbell.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.common.Message;

class GameRoomHintTest {

    @Test
    @DisplayName("1차 힌트 시점에 벨을 누르면 이후 2차 힌트가 공개되어도 벨 누른 순간의 힌트 수(1)로 점수를 계산한다")
    void scoreIsCalculatedByHintCountAtBuzzMoment() throws Exception {
        String sampleData = "단답형|3|피타고라스의 정리는?||피타고라스의 정리\n";
        QuestionDeck deck = new QuestionDeck();
        deck.loadQuestions(new ByteArrayInputStream(sampleData.getBytes(StandardCharsets.UTF_8)));

        Broadcaster noOpBroadcaster = new Broadcaster() {
            @Override
            public void broadcast(Message message) {
            }

            @Override
            public void sendTo(String nickname, Message message) {
            }
        };

        GameRoom room = new GameRoom(noOpBroadcaster, deck);

        room.tryJoin("주성");
        room.joined("주성");
        room.startGame("주성");

        // 1차 힌트 공개 (힌트 수 = 1)
        room.onHintStageReached(1);
        assertEquals(1, room.getRevealedHintCount());

        // 1차 힌트 상태에서 벨 누름 -> 이 순간의 힌트 수(1)가 BellQueue에 기록됨
        room.buzz("주성");

        // 이후 2차 힌트가 공개되어 방의 힌트 수는 2가 되더라도
        room.onHintStageReached(2);
        assertEquals(2, room.getRevealedHintCount());

        // 정답을 맞히면 벨 누른 순간(힌트 1개) 기준으로 3 - 1 = 2점을 얻는다
        room.answer("주성", "피타고라스의 정리");
        assertEquals(2, room.getScoreBoard().getRankedScores().get(0).getValue());
    }
}