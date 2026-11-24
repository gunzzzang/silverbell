package silverbell.common;

/**
 * RESULT 메시지에 담기는 판정 값 상수. 서버({@code GameRoom})가 보내고 화면({@code AnswerPanel})이 읽는다.
 * PRD 9절 및 {@link MessageType#RESULT} 주석과 일치해야 한다. ({@link JoinFailReason}과 같은 방식)
 */
public final class AnswerResult {
    private AnswerResult() {}

    public static final String CORRECT = "정답";
    public static final String WRONG = "오답";
    /** 답변 시간(D4) 안에 답하지 않음. 오답과 같이 감점하고 다음 사람에게 넘긴다(#21) */
    public static final String TIMEOUT = "시간 초과";
}
