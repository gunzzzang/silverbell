package silverbell.server.handler;

import silverbell.common.Message;
import silverbell.server.game.GameRoom;

/**
 * 벨(BUZZ)을 받아 {@link GameRoom#buzz(String)}에 넘긴다 (#14, D1 벨 누르기).
 * 받을지 말지(문제가 떠 있는지, 이미 줄에 있는지)와 줄 순서 알림은 {@code GameRoom}이 정한다.
 */
public class BuzzHandler implements MessageHandler {

    private final GameRoom room;

    public BuzzHandler(GameRoom room) {
        this.room = room;
    }

    @Override
    public void handle(String sender, Message message) {
        room.buzz(sender); // BUZZ는 내용이 없고, 누가 보냈는지만 쓴다
    }
}
