package silverbell.server;

import java.io.IOException;
import java.io.InputStream;

import silverbell.common.GameConfig;
import silverbell.common.MessageType;
import silverbell.server.game.GameRoom;
import silverbell.server.game.QuestionDeck;
import silverbell.server.handler.ChatHandler;
import silverbell.server.handler.MessageDispatcher;
import silverbell.server.handler.StartHandler;
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
        QuestionDeck questionDeck = loadQuestionDeck();
        GameRoom gameRoom = new GameRoom(registry, questionDeck);
        MessageDispatcher dispatcher = new MessageDispatcher();
        registerHandlers(dispatcher, registry, gameRoom);

        try {
            new SocketServer(port, dispatcher, registry, gameRoom).start();
        } catch (IOException e) {
            System.err.println("[서버] 포트 " + port + "를 열 수 없어요. 서버가 이미 켜져 있는지 확인해 주세요. (" + e.getMessage() + ")");
            System.exit(1);
        }
    }

    private static QuestionDeck loadQuestionDeck() {
        QuestionDeck deck = new QuestionDeck();
        try (InputStream in = openQuestionStream()) {
            if (in == null) {
                System.err.println("[서버] 문제 파일을 찾을 수 없어요: " + GameConfig.QUESTION_FILE
                        + " (현재 작업 경로: " + java.nio.file.Path.of("").toAbsolutePath() + ")");
                System.exit(1);
            }
            deck.loadQuestions(in);
            System.out.println("[서버] 문제 " + deck.totalLoadedCount() + "개를 불러왔어요.");
        } catch (IOException e) {
            System.err.println("[서버] 문제 파일을 읽는 중 오류가 났어요: " + e.getMessage());
            System.exit(1);
        }
        return deck;
    }

    private static InputStream openQuestionStream() throws IOException {
        java.nio.file.Path filePath = java.nio.file.Path.of(GameConfig.QUESTION_FILE);
        if (java.nio.file.Files.exists(filePath)) {
            return java.nio.file.Files.newInputStream(filePath);
        }
        return ServerMain.class.getClassLoader().getResourceAsStream(GameConfig.QUESTION_FILE);
    }

    /** 메시지 종류별 핸들러를 등록한다. 서버로 오면 안 되는 종류(SCORE 등)는 등록하지 않아 무시된다(N7). */
    private static void registerHandlers(MessageDispatcher dispatcher, ClientRegistry registry, GameRoom gameRoom) {
        dispatcher.register(MessageType.CHAT, new ChatHandler(registry));
        dispatcher.register(MessageType.START, new StartHandler(gameRoom));
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