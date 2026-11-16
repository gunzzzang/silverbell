package silverbell.server.game.answer;

import java.util.List;

/**
 * 4지선다 채점. 보낸 번호가 정답 번호와 같으면 정답. (PRD E2 서버 채점)
 *
 * <pre>
 * 정답 ["4"]
 * "4" → 정답, " 4 " → 정답, "3" → 오답, "5"·"04"·"넷"·"" → 오답
 * </pre>
 *
 * <p>정답 번호는 {@code QuestionDeck}이 보기를 섞은 뒤의 위치로 이미 바뀌어 있다
 * ({@code Question.shuffleChoices}). 그래서 여기서는 번호만 비교한다.
 */
public class MultipleChoiceChecker implements AnswerChecker {

    /**
     * 앞뒤 공백을 빼고 1~4 중 하나이면서 정답 번호와 같으면 {@code true}.
     * {@code null}·빈 값·1~4가 아닌 값은 예외 없이 {@code false}.
     */
    @Override
    public boolean isCorrect(List<String> answers, String input) {
        if (answers == null || input == null) {
            return false;
        }
        String number = input.trim(); // 앞뒤 공백 제거 (" 4" → "4")
        if (!isChoiceNumber(number)) {
            return false;
        }
        return answers.contains(number);
    }

    /** 1~4인지. {@code Integer.parseInt}는 "넷"에서 예외가 나서 문자열로 비교한다({@code QuestionDeck}과 같은 방식) */
    private boolean isChoiceNumber(String number) {
        return "1".equals(number) || "2".equals(number) || "3".equals(number) || "4".equals(number);
    }
}
