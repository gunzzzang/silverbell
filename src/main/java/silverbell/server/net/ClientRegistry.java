package silverbell.server.net;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import silverbell.common.Message;
import silverbell.server.game.Broadcaster;

/**
 * 입장한 연결 목록과 {@link Broadcaster}의 소켓 구현. 수업 예제의 {@code UserVec}·{@code WriteAll}·{@code WriteOne}을 옮겼다.
 *
 * <p>예제와 다른 점:
 * <ul>
 *   <li>{@code Vector}를 인덱스로 돌다가 중간에 사람이 빠지면 다음 사람을 건너뛰었다.
 *       {@link ConcurrentHashMap}은 도는 중에 빠져도 남은 사람에게 그대로 보낸다.</li>
 *   <li>닉네임을 키로 둬서 중복 닉네임을 {@link #reserve}가 한 번에 막는다(검사와 추가 사이에 끼어들 틈이 없다).</li>
 *   <li>보내다 실패해도 여기서 목록을 고치지 않는다. 실패한 연결은 스스로 닫히고, 그 연결의 받는 스레드가 정리한다.</li>
 * </ul>
 */
public final class ClientRegistry implements Broadcaster {

    private final ConcurrentMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    /**
     * 닉네임을 이 연결 몫으로 잡는다.
     *
     * @return 잡았으면 {@code true}, 이미 다른 연결이 쓰고 있으면 {@code false}
     */
    boolean reserve(String nickname, ClientHandler handler) {
        return clients.putIfAbsent(nickname, handler) == null;
    }

    /** 이 연결이 잡고 있던 닉네임을 놓는다. 다른 연결이 같은 이름을 잡고 있으면 건드리지 않는다. */
    void release(String nickname, ClientHandler handler) {
        clients.remove(nickname, handler);
    }

    /** 입장한 모든 사람에게 보낸다. 한 사람이 실패해도 나머지에게는 보낸다. */
    @Override
    public void broadcast(Message message) {
        for (ClientHandler client : clients.values()) {
            client.send(message);
        }
    }

    /** 입장한 한 사람에게 보낸다. 없는 닉네임이면 아무것도 하지 않는다. */
    @Override
    public void sendTo(String nickname, Message message) {
        ClientHandler client = clients.get(nickname);
        if (client != null) {
            client.send(message);
        }
    }

    /** 지금 입장한 사람 수. */
    public int size() {
        return clients.size();
    }
}
