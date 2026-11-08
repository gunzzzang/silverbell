package silverbell.server.handler;

import silverbell.common.Message;

/**
 * 메시지 한 종류를 처리하는 클래스가 구현한다. 예: {@code BuzzHandler}는 {@code BUZZ}만 처리한다.
 *
 * <p>새 메시지를 처리하려면 이 인터페이스를 구현한 클래스를 만들어
 * {@link MessageDispatcher}에 등록한다. 기존 핸들러는 고치지 않는다.
 */
@FunctionalInterface
public interface MessageHandler {

    /**
     * @param sender  보낸 사람 닉네임. 서버가 접속 때 확인한 값이라 믿을 수 있다(클라이언트가 보낸 값이 아니다).
     *                입장(JOIN) 전 연결의 메시지는 핸들러까지 오지 않는다(#4·#5).
     * @param message 받은 메시지
     */
    void handle(String sender, Message message);
}
