package silverbell.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.common.GameConfig;
import silverbell.common.Message;
import silverbell.common.MessageType;

class GameRoomRoundTest {

    private RecordingBroadcaster broadcaster;
    private QuestionDeck deck;
    private GameRoom room;

    @BeforeEach
    void setUp() throws IOException {
        broadcaster = new RecordingBroadcaster();
        deck = new QuestionDeck();
        String sampleData = """
                4지선다|1|TCP와 UDP 중 연결형 프로토콜은?|TCP;UDP;IP;ARP|1
                단답형|2|서브넷을 구분하기 위해 사용하는 마스크는?|없음|서브넷 마스크/서브넷마스크
                """;
        deck.loadQuestions(new ByteArrayInputStream(sampleData.getBytes(StandardCharsets.UTF_8)));

        room = new GameRoom(broadcaster, deck);
        room.tryJoin("방장");
        room.tryJoin("참가자");
        broadcaster.clear();
    }

    @Test
    @DisplayName("방장이 게임을 시작하면 9개 필드를 담은 QUESTION이 전송되고 라운드가 활성화된다")
    void startGameByHostBroadcastsQuestion() {
        // given: setUp에서 방장과 참가자가 입장해 있고 덱에 문제가 준비된 상태

        // when
        room.startGame("방장");

        // then
        assertTrue(room.isRoundActive());
        assertEquals(0, room.getRevealedHintCount());

        Message questionMsg = broadcaster.lastOfType(MessageType.QUESTION);
        String[] fields = questionMsg.values(9);
        assertEquals(9, fields.length);
        assertEquals("1", fields[0]); // 문제 번호
        assertEquals(String.valueOf(GameConfig.QUESTION_TIME_MS), fields[3]); // 제한 시간
        assertFalse(fields[8].isBlank()); // 마지막 필드: 문제 본문
    }

    @Test
    @DisplayName("방장이 아닌 참가자가 startGame을 호출하면 무시된다")
    void startGameByGuestIsIgnored() {
        // given: 참가자는 방장이 아님

        // when
        room.startGame("참가자");

        // then
        assertFalse(room.isRoundActive());
        assertTrue(broadcaster.sentMessages.isEmpty());
    }

    @Test
    @DisplayName("라운드를 종료하면 REVEAL 메시지가 전송되고 라운드 진행 상태가 false가 된다")
    void finishRoundBroadcastsRevealAndDeactivatesRound() {
        // given
        room.startGame("방장");
        assertTrue(room.isRoundActive());
        broadcaster.clear();

        // when
        room.finishRound();

        // then
        assertFalse(room.isRoundActive());
        assertEquals(1, broadcaster.countOfType(MessageType.REVEAL));
        Message revealMsg = broadcaster.lastOfType(MessageType.REVEAL);
        assertFalse(revealMsg.body().isBlank());
    }

    @Test
    @DisplayName("finishRound를 중복 호출해도 REVEAL 메시지는 한 번만 전송된다")
    void finishRoundCalledTwiceBroadcastsRevealOnce() {
        // given
        room.startGame("방장");
        broadcaster.clear();

        // when: 타이머 만료와 정답 제출이 동시에 일어난 상황
        room.finishRound();
        room.finishRound();

        // then
        assertEquals(1, broadcaster.countOfType(MessageType.REVEAL));
    }

    /** 소켓 대신 보낸 메시지를 메모리에 수집하는 테스트용 Broadcaster */
    private static class RecordingBroadcaster implements Broadcaster {
        private final List<Message> sentMessages = new ArrayList<>();

        @Override
        public void broadcast(Message message) {
            sentMessages.add(message);
        }

        @Override
        public void sendTo(String nickname, Message message) {
            sentMessages.add(message);
        }

        void clear() {
            sentMessages.clear();
        }

        long countOfType(MessageType type) {
            return sentMessages.stream().filter(m -> m.type() == type).count();
        }

        Message lastOfType(MessageType type) {
            return sentMessages.stream()
                    .filter(m -> m.type() == type)
                    .reduce((first, second) -> second)
                    .orElseThrow(() -> new AssertionError(type + " 메시지가 전송되지 않았습니다."));
        }
    }
}