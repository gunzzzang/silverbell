package silverbell.server.game.answer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MultipleChoiceCheckerTest {

    private final AnswerChecker checker = new MultipleChoiceChecker();
    private final List<String> answers = List.of("4"); // 정답은 4번

    @Test
    @DisplayName("정답 번호를 보내면 정답이다")
    void correctNumber() {
        // when, then
        assertTrue(checker.isCorrect(answers, "4"));
    }

    @Test
    @DisplayName("다른 번호를 보내면 오답이다")
    void wrongNumber() {
        // when, then
        assertFalse(checker.isCorrect(answers, "1"));
        assertFalse(checker.isCorrect(answers, "2"));
        assertFalse(checker.isCorrect(answers, "3"));
    }

    @Test
    @DisplayName("앞뒤 공백은 무시한다")
    void ignoreSurroundingSpaces() {
        // when, then
        assertTrue(checker.isCorrect(answers, " 4"));
        assertTrue(checker.isCorrect(answers, "4 "));
        assertTrue(checker.isCorrect(answers, "\t4\n"));
    }

    @Test
    @DisplayName("1~4가 아닌 입력은 오답이다")
    void notChoiceNumber() {
        // when, then
        assertFalse(checker.isCorrect(answers, "0"));
        assertFalse(checker.isCorrect(answers, "5"));
        assertFalse(checker.isCorrect(answers, "04"));
        assertFalse(checker.isCorrect(answers, "4번"));
        assertFalse(checker.isCorrect(answers, "넷"));
        assertFalse(checker.isCorrect(answers, "-4"));
        assertFalse(checker.isCorrect(answers, "4 4"));
    }

    @Test
    @DisplayName("빈 문자열·공백만 보내면 예외 없이 오답이다")
    void emptyInput() {
        // when, then
        assertFalse(checker.isCorrect(answers, ""));
        assertFalse(checker.isCorrect(answers, "   "));
    }

    @Test
    @DisplayName("답이 null이거나 정답 목록이 null·비어 있어도 예외 없이 오답이다")
    void nullInput() {
        // when, then
        assertFalse(checker.isCorrect(answers, null));
        assertFalse(checker.isCorrect(null, "4"));
        assertFalse(checker.isCorrect(new ArrayList<>(), "4"));
    }
}
