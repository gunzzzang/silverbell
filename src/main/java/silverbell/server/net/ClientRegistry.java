package silverbell.server.net;

import java.util.Vector;

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
 *   <li>공개 메서드를 모두 {@code synchronized}로 한다. 예제는 {@code Vector}를 인덱스로 돌다가 중간에 사람이 빠지면
 *       다음 사람을 건너뛰었다. 지금은 보내는 동안 다른 스레드가 목록을 바꾸지 못해서 건너뛰지 않는다.</li>
 *   <li>보내다 실패해도 여기서 목록을 고치지 않는다. 실패한 연결은 스스로 닫히고, 그 연결의 받는 스레드가 정리한다.</li>
 * </ul>
 *
 * <p>한계: 보내기는 받는 쪽이 읽을 때까지 기다릴 수 있다. 클라이언트 프로그램이 멈춰서 읽지 않으면(소켓 버퍼가 가득 차면)
 * 그 사람에게 보내는 동안 다른 입장·퇴장도 기다린다. 2~4명이 한 PC·같은 Wi-Fi에서 하는 과제 범위라 따로 막지 않는다.
 */
public final class ClientRegistry implements Broadcaster {

    /** 입장 검사 중이거나 입장한 이름 (중복 검사용) */
    private final Vector<String> names = new Vector<>();
    /** 입장이 확정된 연결 (보내기용). 예제의 {@code UserVec} */
    private final Vector<ClientHandler> joined = new Vector<>();

    /**
     * 닉네임을 잡는다. 아직 메시지는 받지 않는다.
     *
     * @return 잡았으면 {@code true}, 이미 다른 연결이 쓰고 있으면 {@code false}
     */
    synchronized boolean reserve(String nickname) {
        if (names.contains(nickname)) { // 검사와 추가를 한 번에 해야 같은 이름 둘이 동시에 통과하지 않는다
            return false;
        }
        names.add(nickname);
        return true;
    }

    /** 이름을 잡아 둔 연결을 보내기 목록에 넣는다. {@code JOIN_OK}를 보낸 뒤 부른다. */
    synchronized void activate(ClientHandler handler) {
        joined.add(handler);
    }

    /** 잡아 둔 닉네임을 놓고, 입장했던 연결이면 보내기 목록에서도 뺀다. */
    synchronized void release(String nickname) {
        names.remove(nickname);
        for (int i = 0; i < joined.size(); i++) {
            if (nickname.equals(joined.get(i).nickname())) {
                joined.remove(i);
                return;
            }
        }
    }

    /** 입장한 모든 사람에게 보낸다. 한 사람이 실패해도 나머지에게는 보낸다. 예제의 {@code WriteAll} */
    @Override
    public synchronized void broadcast(Message message) {
        for (int i = 0; i < joined.size(); i++) {
            joined.get(i).send(message); // i번째 사용자에게 전송
        }
    }

    /** 입장한 한 사람에게 보낸다. 없는 닉네임이면 아무것도 하지 않는다. 예제 {@code /to}의 이름 찾기와 같다. */
    @Override
    public synchronized void sendTo(String nickname, Message message) {
        for (int i = 0; i < joined.size(); i++) {
            ClientHandler client = joined.get(i);
            if (nickname.equals(client.nickname())) {
                client.send(message);
                return;
            }
        }
    }

    /** 지금 입장한 사람 수. */
    public synchronized int size() {
        return joined.size();
    }
}
