package silverbell.server.game;

import java.util.ArrayList;
import java.util.List;

import silverbell.common.Message;

/**
 * 테스트용 {@link Broadcaster}. 소켓 없이 보낸 메시지를 모아 두고 확인한다. {@code game} 패키지 테스트에서 같이 쓴다.
 *
 * <pre>
 * // given: 테스트할 클래스에 진짜 소켓 대신 넣는다
 * FakeBroadcaster broadcaster = new FakeBroadcaster();
 * // when: 그 클래스가 broadcaster.broadcast(...) / sendTo(...)를 부른다
 * // then: 무엇을 누구에게 보냈는지 확인한다
 * assertEquals(Message.parse("SYSTEM|이건 님이 입장했어요"), broadcaster.broadcasts().get(0));
 * assertTrue(broadcaster.sentTo("주성").isEmpty());
 * </pre>
 *
 * <p>여러 스레드가 동시에 보내는 테스트에서도 쓸 수 있게 메서드마다 동기화한다.
 */
public class FakeBroadcaster implements Broadcaster {

    /** {@link #sendTo}로 보낸 한 건. */
    public record Sent(String nickname, Message message) {
    }

    private final List<Message> broadcasts = new ArrayList<>();
    private final List<Sent> sent = new ArrayList<>();

    @Override
    public synchronized void broadcast(Message message) {
        broadcasts.add(message);
    }

    @Override
    public synchronized void sendTo(String nickname, Message message) {
        sent.add(new Sent(nickname, message));
    }

    /** 모두에게 보낸 메시지, 보낸 순서대로. */
    public synchronized List<Message> broadcasts() {
        return List.copyOf(broadcasts);
    }

    /** 이 사람에게만 보낸 메시지, 보낸 순서대로. */
    public synchronized List<Message> sentTo(String nickname) {
        return sent.stream().filter(s -> s.nickname().equals(nickname)).map(Sent::message).toList();
    }

    /** 한 사람에게만 보낸 모든 기록, 보낸 순서대로. */
    public synchronized List<Sent> allSent() {
        return List.copyOf(sent);
    }

    /** 지금까지 모은 것을 지운다. 준비 단계에서 보낸 메시지를 빼고 확인할 때 쓴다. */
    public synchronized void clear() {
        broadcasts.clear();
        sent.clear();
    }
}
