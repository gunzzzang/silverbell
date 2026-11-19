package silverbell.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuestionTimerTest {

    @Test
    @DisplayName("타이머를 일시정지하면 시간이 줄지 않고 재개하면 다시 줄어든다")
    void pauseAndResumeTimer() throws InterruptedException {
        // given
        QuestionTimer timer = new QuestionTimer();
        AtomicBoolean timedOut = new AtomicBoolean(false);
        timer.start(1_000, () -> timedOut.set(true));
        Thread.sleep(250);

        // when: 일시정지 후 400ms 대기
        timer.pause();
        long pausedRemaining = timer.getRemainingMs();
        Thread.sleep(400);

        // then: 멈춰있는 동안 남은 시간이 그대로여야 한다
        assertTrue(timer.isPaused());
        assertEquals(pausedRemaining, timer.getRemainingMs());
        assertFalse(timedOut.get());

        // when: 재개 후 나머지 시간 경과
        timer.resume();
        Thread.sleep(900);

        // then: 시간이 0이 되어 콜백이 실행된다
        assertFalse(timer.isPaused());
        assertTrue(timedOut.get());
    }
}