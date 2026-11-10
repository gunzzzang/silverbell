package silverbell.server.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import silverbell.common.Message;
import silverbell.server.game.Broadcaster;

/**
 * 입장한 연결 목록과 {@link Broadcaster}의 소켓 구현. 수업 예제의 {@code UserVec}·{@code WriteAll}·{@code WriteOne}을 옮겼다.
 *
 * <p>닉네임은 두 단계로 관리한다.
 * <ol>
 *   <li>{@link #reserve}: 입장을 검사하는 동안 이름만 잡아 둔다. 같은 이름이 동시에 들어와도 하나만 잡는다.</li>
 *   <li>{@link #activate}: {@code JOIN_OK}를 보낸 뒤 보내기 목록에 넣는다. 그래서 입장이 확정되기 전이나
 *       거절된 연결은 다른 사람에게 가는 메시지를 받지 않고, 새 참가자는 항상 {@code JOIN_OK}를 먼저 받는다.</li>
 * </ol>
 *
 * <p>예제와 다른 점:
 * <ul>
 *   <li>{@code Vector}를 인덱스로 돌다가 중간에 사람이 빠지면 다음 사람을 건너뛰었다.
 *       {@link ConcurrentHashMap}은 도는 중에 빠져도 남은 사람에게 그대로 보낸다.</li>
 *   <li>보내다 실패해도 여기서 목록을 고치지 않는다. 실패한 연결은 스스로 닫히고, 그 연결의 받는 스레드가 정리한다.</li>
 * </ul>
 *
 * <p>한계: 보내기는 받는 쪽이 읽을 때까지 기다릴 수 있다. 클라이언트 프로그램이 멈춰서 읽지 않으면(소켓 버퍼가 가득 차면)
 * 그 사람에게 보내는 스레드도 기다린다. 2~4명이 한 PC·같은 Wi-Fi에서 하는 과제 범위라 따로 막지 않는다.
 */
public final class ClientRegistry implements Broadcaster {

    /** 입장 검사 중이거나 입장한 이름 (중복 검사용) */
    private final ConcurrentMap<String, ClientHandler> names = new ConcurrentHashMap<>();
    /** 입장이 확정된 연결 (보내기용) */
    private final ConcurrentMap<String, ClientHandler> joined = new ConcurrentHashMap<>();

    /**
     * 닉네임을 이 연결 몫으로 잡는다. 아직 메시지는 받지 않는다.
     *
     * @return 잡았으면 {@code true}, 이미 다른 연결이 쓰고 있으면 {@code false}
     */
    boolean reserve(String nickname, ClientHandler handler) {
        return names.putIfAbsent(nickname, handler) == null;
    }

    /** 잡아 둔 닉네임을 보내기 목록에 넣는다. {@code JOIN_OK}를 보낸 뒤 부른다. */
    void activate(String nickname, ClientHandler handler) {
        if (names.get(nickname) == handler) {
            joined.put(nickname, handler);
        }
    }

    /** 이 연결이 잡고 있던 닉네임을 놓는다. 다른 연결이 같은 이름을 잡고 있으면 건드리지 않는다. */
    void release(String nickname, ClientHandler handler) {
        joined.remove(nickname, handler);
        names.remove(nickname, handler);
    }

    /** 입장한 모든 사람에게 보낸다. 한 사람이 실패해도 나머지에게는 보낸다. */
    @Override
    public void broadcast(Message message) {
        for (ClientHandler client : joined.values()) {
            client.send(message);
        }
    }

    /** 입장한 한 사람에게 보낸다. 없는 닉네임이면 아무것도 하지 않는다. */
    @Override
    public void sendTo(String nickname, Message message) {
        ClientHandler client = joined.get(nickname);
        if (client != null) {
            client.send(message);
        }
    }

    /** 지금 입장한 사람 수. */
    public int size() {
        return joined.size();
    }
}
