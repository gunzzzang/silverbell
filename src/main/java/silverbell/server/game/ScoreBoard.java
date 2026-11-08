package silverbell.server.game;

/**
 * 플레이어별 점수. 정답이면 난이도 점수에서 벨을 울린 순간까지 나온 초성 힌트 수를 빼서 더하고(최소 1점),
 * 오답·시간 초과면 감점한다. 점수는 음수가 될 수 있다. (PRD F1 점수 갱신, F2 점수판, Q6)
 *
 * <p>할 일 (M2 · 12주차 · 라인 A, 최종 순위는 M3): 점수 계산, 높은 순 정렬, 최종 순위.
 * 숫자는 {@code GameConfig}의 값을 쓴다.
 */
public class ScoreBoard {
}
