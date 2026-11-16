package silverbell.server.game.answer;

import java.util.List;

/**
 * 단답형 채점. 띄어쓰기·대소문자를 무시하고 정답 후보 중 하나와 같으면 정답. (PRD E2 서버 채점)
 *
 * <pre>
 * 정답 후보 ["서브넷 마스크", "서브넷마스크"]
 * "서브넷마스크" → 정답, " 서브넷  마스크 " → 정답, "서브넷" → 오답
 *
 * 정답 후보 ["TCP"]
 * "tcp" → 정답, "T C P" → 정답
 * </pre>
 *
 * <p>줄임말처럼 띄어쓰기·대소문자로 맞출 수 없는 표기는 문제 파일에 정답 후보로 따로 적는다(#13).
 */
public class ShortAnswerChecker implements AnswerChecker {

    /**
     * 공백을 모두 빼고 대소문자를 무시해 정답 후보 중 하나와 같으면 {@code true}.
     * {@code null}·빈 값·공백만 있는 입력은 예외 없이 {@code false}.
     */
    @Override
    public boolean isCorrect(List<String> answers, String input) {
        if (answers == null || input == null) {
            return false;
        }
        String typed = removeSpaces(input);
        if (typed.isEmpty()) { // 공백만 보낸 답이 빈 정답 후보와 같아지지 않게
            return false;
        }
        for (String answer : answers) {
            if (answer != null && removeSpaces(answer).equalsIgnoreCase(typed)) { // 대소문자 무시 비교
                return true;
            }
        }
        return false;
    }

    /**
     * 띄어쓰기·탭 등 공백 글자를 모두 뺀다. 문장 중간 공백도 빼야 해서 {@code trim()}으로는 부족하다.
     * {@code Character.isWhitespace}는 한글 입력기의 전각 공백도 공백으로 본다.
     */
    private String removeSpaces(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
