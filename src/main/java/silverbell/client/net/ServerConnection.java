package silverbell.client.net;

/**
 * 서버와의 연결 하나. 접속, 메시지 보내기, 받는 스레드에서 한 줄씩 읽어 {@link MessageListener}에게 알리기를 맡는다.
 * 화면 코드는 여기에 쓰지 않는다.
 *
 * <p>할 일 (11주차 뼈대 이슈, PRD A1 서버 접속 · G2 서버 끊김):
 * <ul>
 *   <li>수업 예제의 클라이언트 소켓 코드를 옮겨 온다</li>
 *   <li>보낼 때는 {@code Message.of(...).toLine()}, 받을 때는 {@code Message.parse(line)}</li>
 *   <li>서버가 없거나 끊기면 예외로 창이 꺼지지 않고 {@code onDisconnected()}를 부른다(N5)</li>
 * </ul>
 */
public class ServerConnection {
}
