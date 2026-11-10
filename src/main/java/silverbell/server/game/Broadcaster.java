package silverbell.server.game;

import silverbell.common.Message;

/**
 * 게임 로직이 메시지를 내보낼 때 쓰는 통로.
 *
 * <p>게임 규칙({@code server.game})은 소켓을 직접 모르고 이 인터페이스에만 의존한다.
 * 인터페이스는 쓰는 쪽인 {@code game} 패키지에 두고(DIP), 실제 서버에서는 {@code net} 패키지가 소켓으로 보내도록 구현한다.
 * 그래서 {@code game}은 {@code net}을 import하지 않는다.
 * JUnit 테스트에서는 보낸 메시지를 리스트에 모아두는 가짜 구현을 넣어 소켓 없이 확인한다.
 */
public interface Broadcaster {

    /** 접속한 모든 클라이언트에게 보낸다. */
    void broadcast(Message message);

    /**
     * 입장한 한 사람에게만 보낸다(귓속말, 방장 전용 알림 등). 없는 닉네임이면 아무것도 하지 않는다.
     * 아직 닉네임이 없는 연결에 보내는 {@code JOIN_OK}·{@code JOIN_FAIL}은 {@code net}이 그 연결에 직접 보낸다
     * ({@link ConnectionListener} 참고).
     */
    void sendTo(String nickname, Message message);
}
