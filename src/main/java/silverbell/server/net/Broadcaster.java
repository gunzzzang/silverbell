package silverbell.server.net;

import silverbell.common.Message;

/**
 * 게임 로직이 메시지를 내보낼 때 쓰는 통로.
 *
 * <p>게임 규칙({@code server.game})은 소켓을 직접 모르고 이 인터페이스에만 의존한다.
 * 실제 서버에서는 {@code net} 패키지가 소켓으로 보내도록 구현하고,
 * JUnit 테스트에서는 보낸 메시지를 리스트에 모아두는 가짜 구현을 넣어 소켓 없이 확인한다.
 */
public interface Broadcaster {

    /** 접속한 모든 클라이언트에게 보낸다. */
    void broadcast(Message message);

    /** 한 사람에게만 보낸다(귓속말, 접속 결과, 방장 전용 알림). 없는 닉네임이면 아무것도 하지 않는다. */
    void sendTo(String nickname, Message message);
}
