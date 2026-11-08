package silverbell.client.net;

import silverbell.common.Message;

/**
 * 서버에서 메시지를 받았을 때 알림을 받는 쪽(화면)이 구현한다.
 * Observer 패턴으로, 화면이 통신 구현이 아닌 이 작은 인터페이스에만 의존하게 한다(DIP·ISP).
 *
 * <p>{@link ServerConnection}은 화면을 모르고 이 인터페이스로 알리기만 한다.
 * 그래서 나중에 화면을 통째로 바꿔도(UI 꾸미기) 통신 코드는 고치지 않는다.
 *
 * <p>이 메서드들은 받는 스레드에서 불린다. 화면을 고칠 때는 {@code SwingUtilities.invokeLater}로 넘긴다.
 */
public interface MessageListener {

    void onMessage(Message message);

    /** 서버 연결이 끊겼을 때 (G2 서버 끊김) */
    void onDisconnected();
}
