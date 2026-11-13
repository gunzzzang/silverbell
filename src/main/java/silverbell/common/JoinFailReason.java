package silverbell.common;

/**
 * JOIN_FAIL 메시지에 담기는 실패 사유 상수.
 * PRD 9절 및 {@link MessageType#JOIN_FAIL} 주석과 일치해야 한다.
 */
public final class JoinFailReason {
    private JoinFailReason() {}

    public static final String DUPLICATE_NICKNAME = "닉네임 중복";
    public static final String ROOM_FULL = "인원 초과";
    public static final String GAME_IN_PROGRESS = "게임 진행 중";
    public static final String INVALID_NICKNAME_LENGTH = "닉네임은 1~10자여야 합니다";
    public static final String INVALID_NICKNAME_CHAR = "닉네임에 공백이나 ';', '|', ':'를 쓸 수 없습니다";
}