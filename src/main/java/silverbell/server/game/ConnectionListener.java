package silverbell.server.game;

/**
 * 접속·입장·퇴장을 게임에 알리는 통로. {@code net}이 부르고, 게임 쪽(#5부터 {@code GameRoom})이 구현한다.
 *
 * <p>{@link Broadcaster}와 같은 이유로 쓰는 쪽인 {@code game} 패키지에 둔다(DIP). 그래서 {@code GameRoom}은
 * 소켓을 모른 채 닉네임만 받는다.
 *
 * <p>입장 순서 (한 연결의 받는 스레드에서 차례대로 불린다):
 * <ol>
 *   <li>{@code net}이 닉네임 중복을 먼저 거른다 → 겹치면 {@code JOIN_FAIL|닉네임 중복}</li>
 *   <li>{@link #tryJoin} → 사유를 돌려주면 {@code JOIN_FAIL|사유}</li>
 *   <li>{@code net}이 그 연결에 {@code JOIN_OK}를 보낸다</li>
 *   <li>{@link #joined} → 참가자 목록·입장 안내를 보낸다</li>
 * </ol>
 * 한 번 {@code joined}된 닉네임은 연결이 끊기면 반드시 {@link #left}가 한 번 불린다.
 */
public interface ConnectionListener {

    /**
     * 이 닉네임이 들어와도 되는지 보고, 되면 참가자로 넣는다. 여러 연결이 동시에 부를 수 있으므로
     * 「검사 + 추가」를 구현 쪽에서 한 번에(동기화해서) 해야 인원 제한을 넘지 않는다.
     *
     * @return 들어와도 되면 {@code null}, 안 되면 {@code JOIN_FAIL}에 담을 사유
     */
    String tryJoin(String nickname);

    /** {@code JOIN_OK}를 보낸 뒤 불린다. 여기서 보내는 메시지는 새 참가자도 받는다. */
    void joined(String nickname);

    /** 입장했던 사람의 연결이 끊긴 뒤 불린다. 이미 목록에서 빠져 있어서 그 사람에게는 보낼 수 없다 (#22). */
    void left(String nickname);
}
