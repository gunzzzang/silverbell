package silverbell.server;

/**
 * 서버 시작점. 포트를 열고 접속을 받아 클라이언트마다 {@link silverbell.server.net.ClientHandler} 스레드를 띄운다.
 *
 * <p>할 일 (11주차 뼈대 이슈):
 * <ol>
 *   <li>수업 다중 클라이언트 예제의 서버 코드를 가져와 {@code net} 패키지에 맞게 나눈다</li>
 *   <li>{@code GameRoom}과 {@code MessageDispatcher}를 한 번 만들고 핸들러를 등록한다</li>
 *   <li>{@code ServerSocket}으로 {@link silverbell.common.GameConfig#DEFAULT_PORT}를 연다</li>
 * </ol>
 */
public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) {
        System.out.println("[서버] 아직 구현 전이에요. 11주차 뼈대 이슈에서 수업 예제를 옮겨 와요.");
    }
}
