package silverbell.server.game.answer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.server.game.Question;

class AnswerCheckerSelectorTest {

    private final AnswerCheckerSelector selector = new AnswerCheckerSelector();

    @Test
    @DisplayName("4지선다면 4지선다 채점기, 단답형이면 단답형 채점기를 고른다")
    void selectByType() {
        // when, then
        assertInstanceOf(MultipleChoiceChecker.class, selector.select("4지선다"));
        assertInstanceOf(ShortAnswerChecker.class, selector.select("단답형"));
    }

    @Test
    @DisplayName("모르는 유형이면 IllegalArgumentException")
    void unknownType() {
        // when, then
        assertThrows(IllegalArgumentException.class, () -> selector.select("OX"));
        assertThrows(IllegalArgumentException.class, () -> selector.select(null));
    }

    @Test
    @DisplayName("4지선다 보기를 섞어도 정답 보기가 옮겨 간 번호가 정답이다")
    void multipleChoiceAfterShuffle() {
        // given — 정답은 4번 「전송」
        Question question = new Question("4지선다", "1", "TCP는 몇 계층 프로토콜인가?",
                List.of("물리", "데이터링크", "네트워크", "전송"), List.of("4"));
        question.shuffleChoices(); // QuestionDeck이 출제할 때처럼 보기를 섞는다
        String correctNumber = String.valueOf(question.getChoices().indexOf("전송") + 1);
        String wrongNumber = String.valueOf(question.getChoices().indexOf("물리") + 1);

        // when
        AnswerChecker checker = selector.select(question.getType());

        // then
        assertTrue(checker.isCorrect(question.getAnswers(), correctNumber));
        assertFalse(checker.isCorrect(question.getAnswers(), wrongNumber));
    }

    @Test
    @DisplayName("단답형 문제는 고른 채점기로 정답 후보와 비교한다")
    void shortAnswerQuestion() {
        // given
        Question question = new Question("단답형", "2", "IP 주소에서 네트워크 부분과 호스트 부분을 나누는 값은?",
                List.of(), List.of("서브넷 마스크", "서브넷마스크"));

        // when
        AnswerChecker checker = selector.select(question.getType());

        // then
        assertTrue(checker.isCorrect(question.getAnswers(), " 서브넷  마스크 "));
        assertFalse(checker.isCorrect(question.getAnswers(), "게이트웨이"));
    }
}
