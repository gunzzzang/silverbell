package silverbell.server.game;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 문제 제한 시간을 재는 서버 타이머.
 * 누군가 벨을 눌러 답변 차례가 되면 일시정지하고, 차례가 끝나면 이어서 줄어든다.
 */
public class QuestionTimer {

    private static final long TICK_MS = 100L;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "question-timer");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> task;
    private long remainingMs;
    private volatile boolean paused = false;
    private Runnable onTimeout;

    /**
     * 문제 타이머를 새로 시작한다.
     *
     * @param durationMs 제한 시간(밀리초, {@code GameConfig.QUESTION_TIME_MS})
     * @param onTimeout  남은 시간이 0이 되었을 때 실행할 동작 (예: {@code gameRoom::finishRound})
     */
    public synchronized void start(long durationMs, Runnable onTimeout) {
        stop();
        this.remainingMs = durationMs;
        this.paused = false;
        this.onTimeout = onTimeout;

        this.task = scheduler.scheduleAtFixedRate(this::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        Runnable timeout = null;
        synchronized (this) {
            if (paused || task == null || task.isCancelled()) {
                return;
            }
            remainingMs -= TICK_MS;
            if (remainingMs > 0) {
                return;
            }
            remainingMs = 0;
            stop();
            timeout = onTimeout;
        }
        if (timeout != null) {
            timeout.run(); // 타이머 잠금을 놓은 뒤 호출 (GameRoom 잠금과 엇갈리지 않게)
        }
    }

    /**
     * 문제 타이머를 일시정지한다.
     * <p>사용법: 라인 B(#21)에서 누군가 벨을 눌러 답변 차례(TURN)가 시작될 때 호출한다.
     * 이미 멈춰 있으면 아무 일도 하지 않는다.
     */
    public synchronized void pause() {
        this.paused = true;
    }

    /**
     * 일시정지된 문제 타이머를 이어서 재개한다.
     * <p>사용법: 라인 B(#21)에서 답변 차례가 끝나고 다음 답변자가 없을 때 호출한다.
     * 멈춰 있지 않으면 아무 일도 하지 않는다.
     */
    public synchronized void resume() {
        this.paused = false;
    }

    /**
     * 타이머를 완전히 중단한다. 라운드가 조기 종료(정답, /skip)될 때 호출한다.
     */
    public synchronized void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel(false);
        }
        task = null;
    }

    /** 테스트 및 상태 확인용 남은 시간(밀리초). */
    public synchronized long getRemainingMs() {
        return remainingMs;
    }

    /** 테스트 및 상태 확인용 일시정지 여부. */
    public boolean isPaused() {
        return paused;
    }
}