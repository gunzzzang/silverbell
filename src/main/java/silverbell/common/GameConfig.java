package silverbell.common;

/**
 * 게임 규칙 숫자를 한 곳에 모은 상수. 코드 중간에 숫자를 직접 쓰지 않고 여기 값을 쓴다.
 *
 * <p>시간 값은 모두 임시값이다(PRD Q8). 15주차(#33)에 직접 해보고 여기서만 고친다.
 */
public final class GameConfig {

    private GameConfig() {
    }

    // ── 접속 (PRD Q2) ───────────────────────────
    /** 5000은 macOS AirPlay 수신이 쓰고 있어서 피한다 */
    public static final int DEFAULT_PORT = 5050;
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

    // ── 시간 (밀리초, 임시값) ─────────────────────
    /** C2 문제 타이머 — 답변 차례 동안은 멈춘다 */
    public static final long QUESTION_TIME_MS = 20_000;
    /** D4 답변 시간 */
    public static final long ANSWER_TIME_MS = 10_000;
    /** C3 다음 문제까지 */
    public static final long NEXT_QUESTION_DELAY_MS = 3_000;
    /** A3 게임 시작 카운트다운 (3, 2, 1) */
    public static final int COUNTDOWN_SECONDS = 3;
    /** C4 1차 초성 힌트 — 문제 시간의 약 40% */
    public static final long FIRST_HINT_AT_MS = QUESTION_TIME_MS * 40 / 100;
    /** C4 2차 초성 힌트 — 문제 시간의 약 70% */
    public static final long SECOND_HINT_AT_MS = QUESTION_TIME_MS * 70 / 100;
    /** H9 힌트 투표 — 방장이 열기까지, 투표가 끝나기까지 각각 */
    public static final long HINT_VOTE_TIME_MS = 10_000;

    // ── 점수 (PRD F1, Q6) ───────────────────────
    /** 정답이면 받는 최소 점수 (난이도 - 나온 힌트 수가 이보다 작아도) */
    public static final int MIN_CORRECT_SCORE = 1;
    /** 오답·시간 초과 감점. 점수는 음수가 될 수 있다 */
    public static final int WRONG_PENALTY = 1;
    /** H1 골든벨 문제 점수 배수 */
    public static final int GOLDEN_BELL_MULTIPLIER = 2;

    // ── 한 판 ──────────────────────────────────
    /** Q7 종료 조건 — 한 판 문제 수 (Q11 발표 시간 보고 확정) */
    public static final int QUESTIONS_PER_GAME = 10;
    public static final String QUESTION_FILE = "questions/sample.txt";
}
