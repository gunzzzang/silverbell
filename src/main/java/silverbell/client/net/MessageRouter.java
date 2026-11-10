package silverbell.client.net;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 받은 메시지를 종류별로 나눠 화면·영역에게 준다. 서버의 {@code MessageDispatcher}와 같은 생각이다.
 * 화면이 받고 싶은 종류를 등록하므로, 한 파일의 큰 {@code switch}에 두 라인이 {@code case}를 추가하지 않는다.
 *
 * <pre>
 * MessageRouter router = new MessageRouter();
 * router.on(MessageType.CHAT, message -> chatPanel.append(message));     // 라인 A 영역
 * router.on(MessageType.QUEUE, message -> bellPanel.showQueue(message));  // 라인 B 영역
 * router.whenDisconnected(() -> frame.showDisconnected());
 * ServerConnection.connect(host, port, router);
 * </pre>
 *
 * <p>등록한 코드는 항상 화면 스레드(EDT)에서 불린다. 그래서 받은 코드 안에서 Swing 화면을 바로 고쳐도 된다.
 * (예제는 받는 스레드에서 {@code textArea}를 고쳐 가끔 화면이 깨질 수 있었다.)
 *
 * <p>한 종류에 여러 곳이 등록할 수 있다. 예: {@code SCORE}는 점수판과 결과 화면이 같이 받는다.
 */
public final class MessageRouter implements MessageListener {

    private final Map<MessageType, List<Consumer<Message>>> handlers = new ConcurrentHashMap<>();
    private final List<Runnable> disconnectHandlers = new CopyOnWriteArrayList<>();
    private final Executor uiThread;

    /** 화면 스레드(EDT)로 넘겨 부른다. */
    public MessageRouter() {
        this(SwingUtilities::invokeLater);
    }

    /** 테스트용: 넘길 곳을 정한다({@code Runnable::run}이면 바로 부른다). */
    MessageRouter(Executor uiThread) {
        this.uiThread = uiThread;
    }

    /** 이 종류의 메시지를 받으면 {@code handler}를 부른다. 화면을 만들 때 한 번 등록한다. */
    public void on(MessageType type, Consumer<Message> handler) {
        handlers.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /** 서버 연결이 끊기면 {@code handler}를 부른다 (G2 서버 끊김). */
    public void whenDisconnected(Runnable handler) {
        disconnectHandlers.add(handler);
    }

    /** 등록한 곳이 없는 종류는 무시한다. */
    @Override
    public void onMessage(Message message) {
        List<Consumer<Message>> targets = handlers.get(message.type());
        if (targets == null) {
            return;
        }
        uiThread.execute(() -> targets.forEach(handler -> handler.accept(message)));
    }

    @Override
    public void onDisconnected() {
        uiThread.execute(() -> disconnectHandlers.forEach(Runnable::run));
    }
}
