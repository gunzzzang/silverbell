package silverbell.client.net;

import silverbell.common.Message;

/**
 * 화면이 서버로 메시지를 보낼 때 쓴다. 게임 화면 영역(벨·채팅 입력칸·답 입력)이 {@link ServerConnection}을
 * 직접 몰라도 되게 한다.
 *
 * <p>접속 전에 만든 화면도 쓸 수 있어야 해서, 연결은 이 인터페이스를 구현한 쪽({@code MainFrame})이 들고 있다가
 * 접속돼 있을 때만 보낸다. {@link MessageListener}와 같은 방식이다.
 */
public interface MessageSender {

    /** 서버로 메시지 하나를 보낸다. 접속이 끊겨 있으면 아무 일도 하지 않는다. */
    void send(Message message);
}
