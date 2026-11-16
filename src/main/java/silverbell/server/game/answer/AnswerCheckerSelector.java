package silverbell.server.game.answer;

/**
 * 문제 유형에 맞는 채점기를 골라 준다. (PRD E2 서버 채점)
 *
 * <p>유형을 따지는 곳은 여기 한 군데뿐이다. 답안을 처리하는 쪽(#16)은 아래처럼 고른 채점기의
 * {@code isCorrect}만 부르고, 4지선다인지 단답형인지 몰라도 된다.
 *
 * <pre>
 * AnswerChecker checker = selector.select(question.getType());
 * boolean correct = checker.isCorrect(question.getAnswers(), input);
 * </pre>
 *
 * <p>새 유형(예: OX)이 생기면 채점기 클래스를 만들고 여기에 한 줄만 더한다.
 */
public class AnswerCheckerSelector {

    /** 유형 이름. {@code QuestionDeck}이 문제 파일에서 읽어 {@code Question.getType()}으로 주는 값과 같다 */
    public static final String MULTIPLE_CHOICE = "4지선다";
    public static final String SHORT_ANSWER = "단답형";

    // 채점기는 상태가 없어서 하나씩만 만들어 여러 스레드가 같이 써도 된다
    private final AnswerChecker multipleChoiceChecker = new MultipleChoiceChecker();
    private final AnswerChecker shortAnswerChecker = new ShortAnswerChecker();

    /**
     * 유형에 맞는 채점기를 돌려준다.
     *
     * @throws IllegalArgumentException 모르는 유형. {@code QuestionDeck}이 두 유형만 읽으므로 실제 게임에서는 나오지 않는다
     */
    public AnswerChecker select(String type) {
        if (MULTIPLE_CHOICE.equals(type)) {
            return multipleChoiceChecker;
        }
        if (SHORT_ANSWER.equals(type)) {
            return shortAnswerChecker;
        }
        throw new IllegalArgumentException("채점기가 없는 문제 유형: " + type);
    }
}
