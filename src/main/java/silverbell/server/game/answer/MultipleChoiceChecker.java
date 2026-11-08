package silverbell.server.game.answer;

import java.util.List;

/**
 * 4지선다 채점. 보낸 번호가 정답 번호와 같으면 정답. (PRD E2 서버 채점)
 *
 * <p>할 일 (M2 · 12주차 · 라인 B): 앞뒤 공백 무시, 1~4가 아닌 입력은 오답.
 * {@code QuestionDeck}이 보기 순서를 섞으면 정답 번호도 섞인 위치로 바뀌어 있어야 한다.
 */
public class MultipleChoiceChecker implements AnswerChecker {

    @Override
    public boolean isCorrect(List<String> answers, String input) {
        throw new UnsupportedOperationException("M2(12주차) 라인 B에서 구현");
    }
}
