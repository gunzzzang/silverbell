package silverbell.server.game.answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.server.game.Question;
import silverbell.server.game.QuestionDeck;

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
    @DisplayName("문제 파일에서 읽은 문제를 유형을 따지지 않고 같은 방법으로 채점한다")
    void checkQuestionsFromDeck() throws IOException {
        // given — questions/sample.txt와 같은 형식
        String file = "4지선다|1|TCP는 몇 계층 프로토콜인가?|물리;데이터링크;네트워크;전송|4\n"
                + "단답형|2|IP 주소에서 네트워크 부분과 호스트 부분을 나누는 값은?||서브넷 마스크/서브넷마스크\n";
        QuestionDeck deck = new QuestionDeck();
        deck.loadQuestions(new ByteArrayInputStream(file.getBytes(StandardCharsets.UTF_8)));

        for (int i = 0; i < deck.totalLoadedCount(); i++) {
            Question question = deck.drawNext();
            AnswerChecker checker = selector.select(question.getType());

            if ("4지선다".equals(question.getType())) {
                // 보기가 섞여서 정답 번호가 바뀌어도, 「전송」이 있는 번호가 정답이어야 한다
                String correctNumber = String.valueOf(question.getChoices().indexOf("전송") + 1);
                String wrongNumber = String.valueOf(question.getChoices().indexOf("물리") + 1);

                // when, then
                assertTrue(checker.isCorrect(question.getAnswers(), correctNumber));
                assertFalse(checker.isCorrect(question.getAnswers(), wrongNumber));
            } else {
                // when, then
                assertTrue(checker.isCorrect(question.getAnswers(), " 서브넷  마스크 "));
                assertFalse(checker.isCorrect(question.getAnswers(), "게이트웨이"));
            }
        }
        assertEquals(2, deck.totalLoadedCount());
    }
}
