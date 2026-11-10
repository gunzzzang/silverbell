package silverbell.server;

import java.io.IOException;

import silverbell.common.GameConfig;
import silverbell.common.Message;
import silverbell.common.MessageType;
import silverbell.server.game.ConnectionListener;
import silverbell.server.handler.MessageDispatcher;
import silverbell.server.net.ClientRegistry;
import silverbell.server.net.ServerLog;
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
        MessageDispatcher dispatcher = new MessageDispatcher();
        registerHandlers(dispatcher, registry);

        try {
            new SocketServer(port, dispatcher, registry, temporaryLobby()).start();
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

    /**
     * 임시 입장 처리: 빈 닉네임만 거절한다. #5에서 {@code GameRoom}이 {@link ConnectionListener}를 구현해
     * 인원 초과·게임 진행 중·닉네임 형식 검사와 참가자 목록 보내기를 맡으면 이 메서드를 지운다.
     */
    private static ConnectionListener temporaryLobby() {
        return new ConnectionListener() {
            @Override
            public String tryJoin(String nickname) {
                return nickname.isBlank() ? "닉네임 없음" : null;
            }

            @Override
            public void joined(String nickname) {
                ServerLog.info("입장 " + nickname);
            }

            @Override
            public void left(String nickname) {
                // 목록에서 빼는 건 net이 이미 했다. 게임 처리는 #22.
            }
        };
    }

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
