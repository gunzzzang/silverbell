package silverbell.server.handler;

import silverbell.common.Message;
import silverbell.common.MessageType;
import silverbell.server.game.Broadcaster;

/**
 * 클라이언트 채팅(CHAT) 메시지를 검증하고 발신자 닉네임을 붙여 전체에 전달한다 (B1).
 */
public final class ChatHandler implements MessageHandler {

    private final Broadcaster broadcaster;

    public ChatHandler(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void handle(String sender, Message message) {
        String rawText = message.body();
        if (rawText == null || rawText.isBlank()) {
            return;
        }
        // Message.of()는 줄바꿈 문자를 허용하지 않으므로 방어적으로 공백 치환
        String sanitized = rawText.replace("\r", " ").replace("\n", " ");
        broadcaster.broadcast(Message.of(MessageType.CHAT, sender, sanitized));
    }
}