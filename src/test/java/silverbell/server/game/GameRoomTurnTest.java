package silverbell.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.common.GameConfig;
import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 답변 시간·다음 차례·재도전 금지 테스트 (#21, D4 답변 시간·D5 다음 차례·D6 재도전 금지·C2 문제 타이머).
 *
 * <p>진짜 타이머를 쓰면 답변 시간 10초를 기다려야 해서, 시간을 재지 않고 불린 것만 기록하는
 * {@link RecordingTimer}를 문제 타이머·답변 타이머 자리에 넣는다. 「시간이 다 됨」은 {@link RecordingTimer#timeUp()}으로 흉내 낸다.
 */
class GameRoomTurnTest {

    /** 보통(2점) 단답형 한 문제 */
    private static final String SHORT_QUESTION = "단답형|2|IP 주소에서 네트워크 부분과 호스트 부분을 나누는 값은?||서브넷 마스크/서브넷마스크\n";
    private static final String[] PLAYERS = {"방장", "주성", "이건"};

    private final FakeBroadcaster broadcaster = new FakeBroadcaster();
    private final RecordingTimer questionTimer = new RecordingTimer();
    private final RecordingTimer answerTimer = new RecordingTimer();

    @Test
    @DisplayName("빈 줄에 첫 벨이 들어오면 문제 시간을 멈추고 답변 시간을 처음부터 잰다")
    void firstBuzzPausesQuestionAndStartsAnswerTime() throws IOException {
        // given
        GameRoom room = startedRoom();

        // when
        room.buzz("주성");

        // then
        assertTrue(questionTimer.paused);
        assertTrue(answerTimer.running);
        assertEquals(GameConfig.ANSWER_TIME_MS, answerTimer.startedMs);
    }

    @Test
    @DisplayName("두 번째 사람이 벨을 울리면 답변 시간을 다시 재지 않는다")
    void secondBuzzKeepsAnswerTime() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");

        // when
        room.buzz("이건");

        // then
        assertEquals(1, answerTimer.startCount);
    }

    @Test
    @DisplayName("차례인 사람이 오답이면 다음 사람에게 차례를 넘기고 답변 시간을 다시 잰다")
    void wrongAnswerPassesTurnToNext() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");
        room.buzz("이건");
        broadcaster.clear();

        // when
        room.answer("주성", "게이트웨이");

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "주성 님 오답! -1점 · 이건 님 차례예요"),
                Message.of(MessageType.RESULT, "주성", "오답", "-1"),
                Message.of(MessageType.SCORE, "방장:0", "이건:0", "주성:-1"),
                Message.of(MessageType.QUEUE, "이건"),
                Message.of(MessageType.TURN, "이건", String.valueOf(GameConfig.ANSWER_TIME_MS))
        ), broadcaster.broadcasts());
        assertEquals(2, answerTimer.startCount);
        assertTrue(questionTimer.paused);
    }

    @Test
    @DisplayName("다음 사람이 차례를 넘겨받으면 그 사람의 답을 채점한다")
    void nextPlayerCanAnswer() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");
        room.buzz("이건");
        room.answer("주성", "게이트웨이");
        broadcaster.clear();

        // when
        room.answer("이건", "서브넷 마스크");

        // then
        assertEquals(Message.of(MessageType.RESULT, "이건", "정답", "2"), broadcaster.broadcasts().get(1));
        assertFalse(room.isRoundActive());
    }

    @Test
    @DisplayName("줄에 남은 사람 없이 오답이어도 아직 안 틀린 사람이 있으면 문제 시간을 이어서 재고 답변 시간은 멈춘다")
    void wrongAnswerWithEmptyLineResumesQuestionTime() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");

        // when
        room.answer("주성", "게이트웨이");

        // then
        assertFalse(questionTimer.paused);
        assertFalse(answerTimer.running);
        assertTrue(room.isRoundActive());
    }

    @Test
    @DisplayName("답변 시간이 끝나면 시간 초과로 감점하고 다음 사람에게 차례를 넘긴다")
    void answerTimeUpPassesTurnToNext() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");
        room.buzz("이건");
        broadcaster.clear();

        // when
        answerTimer.timeUp();

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "주성 님 시간 초과! -1점 · 이건 님 차례예요"),
                Message.of(MessageType.RESULT, "주성", "시간 초과", "-1"),
                Message.of(MessageType.SCORE, "방장:0", "이건:0", "주성:-1"),
                Message.of(MessageType.QUEUE, "이건"),
                Message.of(MessageType.TURN, "이건", String.valueOf(GameConfig.ANSWER_TIME_MS))
        ), broadcaster.broadcasts());
    }

    @Test
    @DisplayName("줄에 남은 사람 없이 답변 시간이 끝나도 아직 안 틀린 사람이 있으면 빈 줄·빈 차례를 보내고 문제 시간을 이어서 잰다")
    void answerTimeUpWithEmptyLineResumesQuestionTime() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");
        broadcaster.clear();

        // when
        answerTimer.timeUp();

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "주성 님 시간 초과! -1점"),
                Message.of(MessageType.RESULT, "주성", "시간 초과", "-1"),
                Message.of(MessageType.SCORE, "방장:0", "이건:0", "주성:-1"),
                Message.of(MessageType.QUEUE),
                Message.of(MessageType.TURN)
        ), broadcaster.broadcasts());
        assertFalse(questionTimer.paused);
    }

    @Test
    @DisplayName("방 안 사람이 모두 틀리면 모두 틀렸다는 알림을 보내고 남은 문제 시간을 5초로 다시 잰다")
    void allWrongWaitsShortDelay() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("방장");
        room.buzz("주성");
        room.buzz("이건");
        room.answer("방장", "게이트웨이");
        room.answer("주성", "게이트웨이");
        broadcaster.clear();

        // when
        room.answer("이건", "게이트웨이");

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "이건 님 오답! -1점"),
                Message.of(MessageType.RESULT, "이건", "오답", "-1"),
                Message.of(MessageType.SCORE, "방장:-1", "이건:-1", "주성:-1"),
                Message.of(MessageType.QUEUE),
                Message.of(MessageType.SYSTEM, "모두 틀렸어요! 5초 뒤 정답을 공개해요")
        ), broadcaster.broadcasts());
        assertEquals(GameConfig.ALL_WRONG_REVEAL_DELAY_MS, questionTimer.startedMs);
        assertTrue(questionTimer.running);
        assertTrue(room.isRoundActive());
    }

    @Test
    @DisplayName("마지막 사람이 시간 초과로 틀려도 모두 틀렸으면 남은 문제 시간을 5초로 다시 잰다")
    void allWrongByTimeUpWaitsShortDelay() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("방장");
        room.buzz("주성");
        room.buzz("이건");
        room.answer("방장", "게이트웨이");
        room.answer("주성", "게이트웨이");

        // when
        answerTimer.timeUp(); // 이건이 답하지 않음

        // then
        assertEquals(GameConfig.ALL_WRONG_REVEAL_DELAY_MS, questionTimer.startedMs);
        assertTrue(room.isRoundActive());
    }

    @Test
    @DisplayName("모두 틀린 뒤 5초가 지나면 정답을 공개하고 라운드가 끝난다")
    void allWrongRevealsAfterDelay() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("방장");
        room.buzz("주성");
        room.buzz("이건");
        room.answer("방장", "게이트웨이");
        room.answer("주성", "게이트웨이");
        room.answer("이건", "게이트웨이");
        broadcaster.clear();

        // when
        questionTimer.timeUp(); // 5초가 다 됨

        // then
        assertEquals(List.of(
                Message.of(MessageType.REVEAL, "서브넷 마스크 / 서브넷마스크")
        ), broadcaster.broadcasts());
        assertFalse(room.isRoundActive());
    }

    @Test
    @DisplayName("틀린 사람이 이번 문제에 다시 벨을 울리면 무시된다")
    void ignoreBuzzFromWrongPlayer() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");
        room.answer("주성", "게이트웨이");
        broadcaster.clear();

        // when
        room.buzz("주성");

        // then
        assertEquals(List.of(), broadcaster.broadcasts());
        assertFalse(questionTimer.paused);
    }

    @Test
    @DisplayName("줄이 빈 뒤 아직 안 울린 사람이 벨을 울리면 그 사람 차례가 된다")
    void notYetBuzzedPlayerGetsTurnAfterLineEmpties() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");
        room.answer("주성", "게이트웨이");
        broadcaster.clear();

        // when
        room.buzz("이건");

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "이건 님이 벨을 울렸어요!"),
                Message.of(MessageType.QUEUE, "이건"),
                Message.of(MessageType.TURN, "이건", String.valueOf(GameConfig.ANSWER_TIME_MS))
        ), broadcaster.broadcasts());
        assertTrue(questionTimer.paused);
    }

    @Test
    @DisplayName("정답이 나오면 답변 시간을 멈춘다")
    void correctAnswerStopsAnswerTime() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");

        // when
        room.answer("주성", "서브넷 마스크");

        // then
        assertFalse(answerTimer.running);
    }

    @Test
    @DisplayName("정답을 보낸 뒤 늦게 온 시간 초과는 무시된다")
    void ignoreLateTimeUpAfterCorrectAnswer() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");
        room.answer("주성", "서브넷 마스크");
        broadcaster.clear();

        // when
        answerTimer.timeUp(); // 정답과 거의 같은 순간 시간이 다 돼서 타이머가 이미 부르러 나온 경우

        // then
        assertEquals(List.of(), broadcaster.broadcasts());
    }

    @Test
    @DisplayName("오답을 보낸 뒤 늦게 온 그 사람의 시간 초과는 무시된다")
    void ignoreLateTimeUpAfterWrongAnswer() throws IOException {
        // given
        GameRoom room = startedRoom();
        room.buzz("주성");
        room.answer("주성", "게이트웨이"); // 줄이 비어 답변 타이머를 다시 시작하지 않음 → 주성의 시간 초과가 남아 있음
        broadcaster.clear();

        // when
        answerTimer.timeUp();

        // then
        assertEquals(List.of(), broadcaster.broadcasts());
    }

    /** 세 명이 입장하고 방장이 게임을 시작해 단답형 한 문제가 떠 있는 방. 타이머 두 개는 기록용으로 바꿔 넣는다. */
    private GameRoom startedRoom() throws IOException {
        QuestionDeck deck = new QuestionDeck();
        deck.loadQuestions(new ByteArrayInputStream(SHORT_QUESTION.getBytes(StandardCharsets.UTF_8)));
        GameRoom room = new GameRoom(broadcaster, deck, questionTimer, answerTimer);
        for (String nickname : PLAYERS) {
            room.tryJoin(nickname); // 첫 사람(방장)이 방장이 된다
            room.joined(nickname);  // 점수판에 0점으로 오른다 (#12)
        }
        room.startGame("방장");
        return room;
    }

    /**
     * 시간을 재지 않고 불린 것만 기록하는 타이머. 부모 메서드를 부르지 않아서 타이머 스레드가 생기지 않는다.
     * {@link #timeUp()}을 부르면 마지막으로 {@code start}에 넘긴 동작을 실행해 「시간이 다 됨」을 흉내 낸다.
     */
    private static class RecordingTimer extends QuestionTimer {
        long startedMs;
        int startCount;
        boolean running;
        boolean paused;
        private Runnable onTimeout;

        @Override
        public synchronized void start(long durationMs, Runnable onTimeout) {
            this.startedMs = durationMs;
            this.startCount++;
            this.running = true;
            this.paused = false;
            this.onTimeout = onTimeout;
        }

        @Override
        public synchronized void pause() {
            paused = true;
        }

        @Override
        public synchronized void resume() {
            paused = false;
        }

        @Override
        public synchronized void stop() {
            running = false;
        }

        /** 진짜 타이머처럼 잠금 밖에서 시간 초과 동작을 부른다. */
        void timeUp() {
            Runnable timeout;
            synchronized (this) {
                timeout = onTimeout;
            }
            timeout.run();
        }
    }
}
