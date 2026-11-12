package silverbell.client.net;

import silverbell.common.Message;

/**
 * 한 종류의 메시지를 받는 화면·영역이 구현한다. {@link MessageRouter#on}으로 등록한다.
 * 서버의 {@code MessageHandler}와 같은 역할이고, 화면 스레드(EDT)에서 불린다.
 *
 * <pre>router.on(MessageType.CHAT, message -> chatPanel.append(message));</pre>
 */
public interface MessageReceiver {

    void receive(Message message);
}
