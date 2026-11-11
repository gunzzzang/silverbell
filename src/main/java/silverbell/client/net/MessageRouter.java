package silverbell.client.net;

import java.util.EnumMap;
import java.util.Vector;

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
 * 등록은 {@code ServerConnection.connect} 전에 마친다. 접속 직후 오는 {@code JOIN_OK} 등을 놓치지 않게 하기 위해서다.
 */
public final class MessageRouter implements MessageListener {

    private final EnumMap<MessageType, Vector<MessageReceiver>> receivers = new EnumMap<>(MessageType.class);
    private final Vector<Runnable> disconnectHandlers = new Vector<>();
    /** {@code true}면 EDT로 넘겨 부르고, {@code false}면 받은 스레드에서 바로 부른다(테스트용). */
    private final boolean onUiThread;

    /** 화면 스레드(EDT)로 넘겨 부른다. */
    public MessageRouter() {
        this(true);
    }

    /** 테스트용: {@code false}면 EDT를 거치지 않고 바로 부른다. */
    MessageRouter(boolean onUiThread) {
        this.onUiThread = onUiThread;
    }

    /** 이 종류의 메시지를 받으면 {@code receiver}를 부른다. 화면을 만들 때 한 번 등록한다. */
    public synchronized void on(MessageType type, MessageReceiver receiver) {
        Vector<MessageReceiver> list = receivers.get(type);
        if (list == null) {
            list = new Vector<>();
            receivers.put(type, list);
        }
        list.add(receiver);
    }

    /** 서버 연결이 끊기면 {@code handler}를 부른다 (G2 서버 끊김). */
    public void whenDisconnected(Runnable handler) {
        disconnectHandlers.add(handler);
    }

    /** 등록한 곳이 없는 종류는 무시한다. 받는 스레드에서 불린다. */
    @Override
    public void onMessage(Message message) {
        Vector<MessageReceiver> targets;
        synchronized (this) { // 등록(EDT)과 찾기(받는 스레드)가 다른 스레드라서
            targets = receivers.get(message.type());
        }
        if (targets == null) {
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < targets.size(); i++) {
                    targets.get(i).receive(message);
                }
            }
        });
    }

    /** 받는 스레드에서 불린다. */
    @Override
    public void onDisconnected() {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < disconnectHandlers.size(); i++) {
                    disconnectHandlers.get(i).run();
                }
            }
        });
    }

    private void runOnUi(Runnable task) {
        if (onUiThread) {
            SwingUtilities.invokeLater(task); // Swing 화면은 EDT에서 고쳐야 안전하다
        } else {
            task.run();
        }
    }
}
