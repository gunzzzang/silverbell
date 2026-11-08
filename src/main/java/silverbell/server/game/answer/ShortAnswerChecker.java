package silverbell.server.game.answer;

import java.util.List;

/**
 * 단답형 채점. 띄어쓰기·대소문자를 무시하고 정답 후보 중 하나와 같으면 정답. (PRD E2 서버 채점)
 *
 * <pre>
 * 정답 후보 ["서브넷 마스크", "서브넷마스크"]
 * "서브넷마스크" → 정답, " 서브넷  마스크 " → 정답, "서브넷" → 오답
 * </pre>
 *
 * <p>할 일 (M2 · 12주차 · 라인 B).
 */
public class ShortAnswerChecker implements AnswerChecker {

    @Override
    public boolean isCorrect(List<String> answers, String input) {
        throw new UnsupportedOperationException("M2(12주차) 라인 B에서 구현");
    }
}
