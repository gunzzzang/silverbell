package silverbell.server.handler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 받은 메시지를 종류에 맞는 {@link MessageHandler}에게 넘긴다.
 *
 * <p>큰 {@code switch} 대신 종류별 핸들러를 등록해 두는 방식이다. 기능을 추가할 때
 * 이 클래스와 다른 핸들러를 고치지 않고 핸들러 클래스 하나와 등록 한 줄만 추가한다.
 *
 * <pre>
 * MessageDispatcher dispatcher = new MessageDispatcher();
 * dispatcher.register(MessageType.BUZZ, new BuzzHandler(room));
 * dispatcher.dispatch("이건", Message.parse(line));
 * </pre>
 *
 * <p>등록은 서버 시작 때 한 번만 하고, 그 뒤로는 여러 {@code ClientHandler} 스레드가
 * {@link #dispatch}만 부른다. 실행 중에 등록을 바꾸지 않는다.
 */
public class MessageDispatcher {

    private final Map<MessageType, MessageHandler> handlers = new EnumMap<>(MessageType.class);

    /**
     * 종류별 핸들러를 등록한다.
     *
     * @throws IllegalStateException 같은 종류를 두 번 등록할 때 (둘이 같은 메시지를 따로 처리하는 실수 방지)
     */
    public void register(MessageType type, MessageHandler handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        if (handlers.putIfAbsent(type, handler) != null) {
            throw new IllegalStateException(type + " 핸들러가 이미 등록되어 있다");
        }
    }

    /**
     * 메시지를 맞는 핸들러에게 넘긴다.
     *
     * @return 처리했으면 {@code true}, 등록된 핸들러가 없으면 {@code false}.
     *         서버로 오면 안 되는 메시지(클라이언트가 보낸 가짜 {@code SCORE} 등)는 등록하지 않아 자연스럽게 무시된다(N7).
     */
    public boolean dispatch(String sender, Message message) {
        MessageHandler handler = handlers.get(message.type());
        if (handler == null) {
            return false;
        }
        handler.handle(sender, message);
        return true;
    }
}
