package silverbell.server.handler;

import silverbell.common.Message;
import silverbell.server.game.GameRoom;

/**
 * 방장이 보낸 START 메시지를 받아 게임을 시작한다 (#11 임시 연결, #19에서 준비 조건 추가).
 */
public class StartHandler implements MessageHandler {

    private final GameRoom room;

    public StartHandler(GameRoom room) {
        this.room = room;
    }

    @Override
    public void handle(String sender, Message message) {
        room.startGame(sender);
    }
}