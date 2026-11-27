package silverbell.server.game;

import java.util.List;

/**
 * 단답형 정답으로 초성 힌트를 만든다. (PRD C4 초성 힌트)
 *
 * <pre>
 * 1차: 글자 수 + 첫 글자 초성   "피타고라스의 정리" → "ㅍ○○○○○ ○○"
 * 2차: 초성 전체                "피타고라스의 정리" → "ㅍㅌㄱㄹㅅㅇ ㅈㄹ"
 * </pre>
 *
 * <p>띄어쓰기는 그대로 두고, 한글이 아닌 글자는 ○로 가린다. 정답 후보 중 첫 번째로 만든다.
 *
 * <p>할 일 (#18 · 라인 A): 한글 음절에서 초성 뽑기({@code (글자 - 0xAC00) / 588}), 두 단계 힌트.
 * 소켓·타이머와 상관없는 순수 계산이라 JUnit으로 경계값까지 테스트한다.
 */
public class HintMaker {

    private static final char[] CHOSUNG = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ',
            'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };
    private static final char MASK = '○';
    private static final char HANGUL_START = 0xAC00;
    private static final char HANGUL_END = 0xD7A3;
    private static final int CHOSUNG_UNIT = 588; // 21 * 28

    /**
     * 문제에서 지정한 단계(1차 또는 2차)의 초성 힌트를 만든다.
     * 4지선다이거나 정답이 비어 있으면 빈 문자열을 돌려준다.
     */
    public static String make(Question question, int stage) {
        if (question == null || !"단답형".equals(question.getType())) {
            return "";
        }
        List<String> answers = question.getAnswers();
        if (answers.isEmpty()) {
            return "";
        }
        return make(answers.get(0), stage);
    }

    /**
     * 정답 문자열로 지정한 단계(1차 또는 2차)의 초성 힌트를 만든다.
     */
    public static String make(String answer, int stage) {
        if (answer == null || answer.isEmpty() || (stage != 1 && stage != 2)) {
            return "";
        }

        StringBuilder result = new StringBuilder(answer.length());
        for (int i = 0; i < answer.length(); i++) {
            char ch = answer.charAt(i);
            if (ch == ' ') {
                result.append(' ');
            } else if (stage == 1 && i > 0) {
                result.append(MASK);
            } else {
                result.append(extractChosungOrMask(ch));
            }
        }
        return result.toString();
    }

    private static char extractChosungOrMask(char ch) {
        if (ch >= HANGUL_START && ch <= HANGUL_END) {
            return CHOSUNG[(ch - HANGUL_START) / CHOSUNG_UNIT];
        }
        return MASK;
    }
}