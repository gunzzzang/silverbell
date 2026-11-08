package silverbell.server.net;

/**
 * 접속한 클라이언트 한 명을 맡는 스레드. 소켓에서 한 줄씩 읽어 {@code MessageDispatcher}로 넘기고,
 * 보낼 메시지를 소켓에 쓴다. 게임 규칙은 여기에 쓰지 않는다.
 *
 * <p>할 일 (#4 · 라인 B, 끊김 처리는 #22 · PRD A1 서버 접속 · G1 참가자 퇴장):
 * <ul>
 *   <li>수업 예제의 클라이언트 처리 스레드를 옮겨 온다</li>
 *   <li>받은 줄을 {@code Message.parse}로 바꾸고, 모르는 타입이면 무시한다</li>
 *   <li>연결이 끊기면 {@code GameRoom}에 퇴장을 알린다 (예외로 서버 전체가 멈추지 않게)</li>
 *   <li>받은·보낸 메시지를 시각과 함께 콘솔에 찍는다 (통합 테스트·보고서용 로그)</li>
 * </ul>
 */
public class ClientHandler {
}
