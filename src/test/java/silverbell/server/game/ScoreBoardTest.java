package silverbell.server.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScoreBoardTest {

    private ScoreBoard scoreBoard;

    @BeforeEach
    void setUp() {
        scoreBoard = new ScoreBoard();
        scoreBoard.addPlayer("User1");
        scoreBoard.addPlayer("User2");
        scoreBoard.addPlayer("User3");
    }

    @Test
    @DisplayName("난이도(1, 2, 3)에 따라 기본 점수가 정확히 계산된다")
    void addCorrectScore_byDifficulty() {
        assertEquals(1, scoreBoard.addCorrectScore("User1", "1", 0));
        assertEquals(2, scoreBoard.addCorrectScore("User2", "2", 0));
        assertEquals(3, scoreBoard.addCorrectScore("User3", "3", 0));

        assertArrayEquals(
                new String[]{"User3:3", "User2:2", "User1:1"},
                scoreBoard.toScoreTokens()
        );
    }

    @Test
    @DisplayName("힌트 수를 뺀 점수가 0 이하가 되어도 최소 1점을 보장한다")
    void addCorrectScore_minimumOnePoint() {
        // 난이도 2 - 힌트 2개 = 0 -> 최소 1점
        int earned1 = scoreBoard.addCorrectScore("User1", "2", 2);
        // 난이도 1 - 힌트 3개 = -2 -> 최소 1점
        int earned2 = scoreBoard.addCorrectScore("User2", "1", 3);

        assertEquals(1, earned1);
        assertEquals(1, earned2);
    }

    @Test
    @DisplayName("오답 및 시간 초과 시 1점씩 감점되며 음수 점수를 허용한다")
    void applyPenalty_allowsNegativeScore() {
        assertEquals(-1, scoreBoard.applyPenalty("User1"));
        assertEquals(-1, scoreBoard.applyPenalty("User1"));

        List<Map.Entry<String, Integer>> ranked = scoreBoard.getRankedScores();
        Map.Entry<String, Integer> last = ranked.get(ranked.size() - 1);
        assertEquals("User1", last.getKey());
        assertEquals(-2, last.getValue());
    }

    @Test
    @DisplayName("점수 목록은 높은 순으로 정렬되고, 나간 사람은 점수판에서 제거된다")
    void getRankedScores_and_removePlayer() {
        scoreBoard.addCorrectScore("User1", "1", 0); // 1점
        scoreBoard.addCorrectScore("User2", "3", 0); // 3점
        scoreBoard.applyPenalty("User3");             // -1점

        assertArrayEquals(
                new String[]{"User2:3", "User1:1", "User3:-1"},
                scoreBoard.toScoreTokens()
        );

        scoreBoard.removePlayer("User2");
        assertArrayEquals(
                new String[]{"User1:1", "User3:-1"},
                scoreBoard.toScoreTokens()
        );
    }
}