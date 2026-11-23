package silverbell.server.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import silverbell.common.GameConfig;

/**
 * 플레이어별 점수. 정답이면 난이도 점수에서 벨을 울린 순간까지 나온 초성 힌트 수를 빼서 더하고(최소 1점),
 * 오답·시간 초과면 감점한다. 점수는 음수가 될 수 있다. (PRD F1 점수 갱신, F2 점수판, Q6)
 *
 * <p>할 일 (#12 · 라인 A, 최종 순위는 #20): 점수 계산, 높은 순 정렬, 최종 순위.
 * 숫자는 {@code GameConfig}의 값을 쓴다.
 */
public class ScoreBoard {
    private final Map<String, Integer> scores = new LinkedHashMap<>();

    /**
     * 참가자 입장 시 0점으로 등록한다. 이미 있으면 유지한다.
     */
    public synchronized void addPlayer(String nickname) {
        if (nickname != null && !nickname.isBlank()) {
            scores.putIfAbsent(nickname, 0);
        }
    }

    /**
     * 정답 처리 (#16이 판정 후 호출).
     * 규칙: 난이도 점수 - 벨을 울린 순간까지 나온 힌트 수, 최소 {@link GameConfig#MIN_CORRECT_SCORE}점.
     *
     * @param nickname        정답자 닉네임
     * @param difficulty      문제 난이도 ("1", "2", "3" 등 정수 문자열)
     * @param hintCountAtBuzz 벨을 울린 순간까지 공개된 힌트 수 (#14가 기록해 #16이 전달)
     * @return 이번 정답으로 얻은 점수
     */
    public synchronized int addCorrectScore(String nickname, String difficulty, int hintCountAtBuzz) {
        int baseScore = parseDifficultyScore(difficulty);
        int earned = Math.max(GameConfig.MIN_CORRECT_SCORE, baseScore - Math.max(0, hintCountAtBuzz));
        scores.merge(nickname, earned, Integer::sum);
        return earned;
    }

    /**
     * 오답·시간 초과 감점 (#16 또는 답변 타이머 만료 시 호출).
     * 규칙: {@link GameConfig#WRONG_PENALTY}만큼 감점, 음수 허용.
     *
     * @param nickname 감점 대상 닉네임
     * @return 변동된 점수 (음수, 예: -1)
     */
    public synchronized int applyPenalty(String nickname) {
        int delta = -GameConfig.WRONG_PENALTY;
        scores.merge(nickname, delta, Integer::sum);
        return delta;
    }

    /**
     * 현재 점수 목록을 높은 점수 순(내림차순)으로 반환한다.
     * 동점일 경우 닉네임 오름차순으로 안정 정렬한다.
     */
    public synchronized List<Map.Entry<String, Integer>> getRankedScores() {
        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(scores.entrySet());
        ranked.sort((a, b) -> {
            int scoreCompare = Integer.compare(b.getValue(), a.getValue());
            return (scoreCompare != 0) ? scoreCompare : a.getKey().compareTo(b.getKey());
        });
        return ranked;
    }

    /**
     * 나간 사람 빼기 (#22 퇴장 처리에서 호출).
     */
    public synchronized void removePlayer(String nickname) {
        scores.remove(nickname);
    }

    /**
     * {@code MessageType.SCORE} 전송용 {@code 닉네임:점수} 배열을 높은 점수 순으로 반환한다.
     */
    public synchronized String[] toScoreTokens() {
        List<Map.Entry<String, Integer>> ranked = getRankedScores();
        String[] tokens = new String[ranked.size()];
        for (int i = 0; i < ranked.size(); i++) {
            Map.Entry<String, Integer> entry = ranked.get(i);
            tokens[i] = entry.getKey() + ":" + entry.getValue();
        }
        return tokens;
    }

    private int parseDifficultyScore(String difficulty) {
        try {
            return Integer.parseInt(difficulty.strip());
        } catch (NumberFormatException | NullPointerException e) {
            return GameConfig.MIN_CORRECT_SCORE;
        }
    }
}
