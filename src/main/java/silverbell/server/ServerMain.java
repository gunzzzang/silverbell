package silverbell.server;

import java.io.IOException;

import silverbell.common.GameConfig;
import silverbell.common.Message;
import silverbell.common.MessageType;
import silverbell.server.game.GameRoom;
import silverbell.server.handler.MessageDispatcher;
import silverbell.server.net.ClientRegistry;
import silverbell.server.net.SocketServer;

/**
 * 서버 시작점. 객체를 한 번씩 만들어 서로 연결하고, 메시지 핸들러를 등록하고, 포트를 연다.
 *
 * <p>핸들러 등록은 이 클래스 한 곳에서만 한다. 기능을 추가할 때는 핸들러 클래스를 만들고
 * {@link #registerHandlers}에 한 줄 추가한다. 이 파일은 두 라인이 같이 고치므로 그 줄만 작은 PR로 올린다.
 *
 * <pre>./gradlew runServer            (기본 포트 {@value GameConfig#DEFAULT_PORT})
 * ./gradlew runServer --args=6000  (다른 포트)</pre>
 */
public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) {
        int port = parsePort(args);
        ClientRegistry registry = new ClientRegistry();
        GameRoom gameRoom = new GameRoom(registry); // 1. 임시 로비 대신 실제 GameRoom 생성
        MessageDispatcher dispatcher = new MessageDispatcher();
        registerHandlers(dispatcher, registry);

        try {
            // 2. temporaryLobby() 대신 gameRoom 전달
            new SocketServer(port, dispatcher, registry, gameRoom).start();
        } catch (IOException e) {
            System.err.println("[서버] 포트 " + port + "를 열 수 없어요. 서버가 이미 켜져 있는지 확인해 주세요. (" + e.getMessage() + ")");
            System.exit(1);
        }
    }

    /** 메시지 종류별 핸들러를 등록한다. 서버로 오면 안 되는 종류(SCORE 등)는 등록하지 않아 무시된다(N7). */
    private static void registerHandlers(MessageDispatcher dispatcher, ClientRegistry registry) {
        // 임시: #6에서 ChatHandler(정답 가리기 포함)로 바꾼다. 그때까지는 접속 테스트용으로 받은 그대로 모두에게 전달한다.
        dispatcher.register(MessageType.CHAT,
                (sender, message) -> registry.broadcast(Message.of(MessageType.CHAT, sender, message.body())));
    }

    // 3. temporaryLobby() 메서드 및 안 쓰는 import(ConnectionListener, ServerLog) 삭제

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return GameConfig.DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("[서버] 포트는 숫자여야 해요: " + args[0] + " → 기본 포트로 시작해요.");
            return GameConfig.DEFAULT_PORT;
        }
    }
}