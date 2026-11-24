package silverbell.server.handler;

import silverbell.common.Message;
import silverbell.server.game.GameRoom;

/**
 * 답안(ANSWER)을 받아 {@link GameRoom#answer(String, String)}에 넘긴다 (#16, E1 답안 입력).
 * 받을지 말지(문제가 떠 있는지, 지금 차례인지)와 채점·판정 알림은 {@code GameRoom}이 정한다.
 */
public class AnswerHandler implements MessageHandler {

    private final GameRoom room;

    public AnswerHandler(GameRoom room) {
        this.room = room;
    }

    @Override
    public void handle(String sender, Message message) {
        room.answer(sender, message.body()); // 답안은 값 하나라 내용 전체가 답 (단답형 글에 ';'가 있어도 그대로)
    }
}
