package silverbell.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.common.Message;
import silverbell.common.MessageType;

class MessageDispatcherTest {

    @Test
    @DisplayName("등록한 종류의 메시지는 그 핸들러가 보낸 사람과 함께 받는다")
    void dispatchToRegisteredHandler() {
        // given
        MessageDispatcher dispatcher = new MessageDispatcher();
        List<String> handled = new ArrayList<>();
        dispatcher.register(MessageType.BUZZ, (sender, message) -> handled.add(sender + ":" + message.type()));

        // when
        boolean result = dispatcher.dispatch("이건", Message.parse("BUZZ"));

        // then
        assertTrue(result);
        assertEquals(List.of("이건:BUZZ"), handled);
    }

    @Test
    @DisplayName("핸들러가 없는 종류는 무시된다")
    void ignoreUnregistered() {
        // given
        MessageDispatcher dispatcher = new MessageDispatcher();

        // when
        boolean result = dispatcher.dispatch("이건", Message.parse("SCORE|이건;9999"));

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("같은 종류를 두 번 등록하면 예외가 난다")
    void rejectDuplicateRegistration() {
        // given
        MessageDispatcher dispatcher = new MessageDispatcher();
        dispatcher.register(MessageType.CHAT, (sender, message) -> { });

        // when, then
        assertThrows(IllegalStateException.class,
                () -> dispatcher.register(MessageType.CHAT, (sender, message) -> { }));
    }
}
