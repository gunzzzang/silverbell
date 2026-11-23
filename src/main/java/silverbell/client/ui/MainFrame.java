package silverbell.client.ui;

import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import silverbell.client.net.MessageRouter;
import silverbell.client.net.MessageSender;
import silverbell.client.net.ServerConnection;
import silverbell.client.ui.game.GameScreen;
import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 게임 창 하나. {@link CardLayout}으로 타이틀·대기실·게임·결과 화면을 겹쳐 두고 하나만 보여준다.
 * 화면마다 창을 새로 띄우지 않아서 위치·크기가 유지되고 전환이 자연스럽다.
 *
 * <p>할 일: 각 화면 패널을 UI 명세서대로 채운다 (#20 결과 화면 → #26·#27 꾸미기).
 * 게임 화면은 {@link GameScreen}이 영역별로 나눠 맡는다(#7).
 */
public final class MainFrame extends JFrame {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 640;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    /** 모든 화면이 같이 쓰는 메시지 분배기. 화면들이 받을 메시지를 등록한 뒤 이걸로 서버에 접속한다 */
    private final MessageRouter router = new MessageRouter();
    private final TitlePanel titlePanel;
    private final LobbyPanel lobbyPanel;
    private final GameScreen gameScreen = new GameScreen(router, new MessageSender() {
        @Override
        public void send(Message message) { // 게임 화면은 연결을 모르고 이걸로만 보낸다 (#14)
            sendMessage(message);
        }
    });
    private ServerConnection connection;
    /** 지금 보이는 화면 */
    private Screen currentScreen;

    public MainFrame() {
        super("실버벨");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        titlePanel = new TitlePanel(router, this::onConnected);
        lobbyPanel = new LobbyPanel(this::sendMessage);

        registerMessageHandlers();

        addScreen(Screen.TITLE, titlePanel);
        addScreen(Screen.LOBBY, lobbyPanel);
        addScreen(Screen.GAME, gameScreen);
        addScreen(Screen.RESULT, new PlaceholderScreen("결과 · 최종 순위 (F3)"));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeConnection();
            }
        });

        setContentPane(root);
        show(Screen.TITLE);
    }

    /** 접속 전에 받을 메시지 처리를 모두 등록한다(등록된 콜백은 EDT에서 실행됨). */
    private void registerMessageHandlers() {
        router.on(MessageType.JOIN_OK, message -> {
            lobbyPanel.reset();
            lobbyPanel.setMyNickname(titlePanel.getNickname());
            gameScreen.setMyNickname(titlePanel.getNickname());
            titlePanel.onJoinSuccess();
            show(Screen.LOBBY);
        });

        router.on(MessageType.JOIN_FAIL, message -> titlePanel.onJoinFailed(message.body()));

        router.on(MessageType.USERS, lobbyPanel::updateUsers);
        router.on(MessageType.CHAT, lobbyPanel::appendChat);
        router.on(MessageType.SYSTEM, lobbyPanel::appendSystem);

        // #11 임시 화면 전환: 첫 QUESTION이 오면 모든 참가자를 게임 화면으로 전환한다 (#19에서 COUNTDOWN으로 교체)
        router.on(MessageType.QUESTION, message -> show(Screen.GAME));

        router.whenDisconnected(() -> {
            connection = null;
            show(Screen.TITLE);
            titlePanel.showError("서버와의 연결이 끊어졌습니다.");
        });
    }

    private void onConnected(ServerConnection conn) {
        this.connection = conn;
    }

    private void sendMessage(Message message) {
        if (connection != null) {
            connection.send(message);
        }
    }

    private void closeConnection() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    /** 보여줄 화면을 바꾼다. EDT에서 불러야 한다. 이미 그 화면이면 아무 일도 하지 않는다. */
    public void show(Screen screen) {
        if (screen == currentScreen) {
            return; // 문제마다 show(GAME)이 불려도, 채팅 치는 중인 입력칸의 포커스를 뺏지 않게 (#14)
        }
        currentScreen = screen;
        cards.show(root, screen.name());
        if (screen == Screen.GAME) {
            gameScreen.takeKeyFocus(); // 스페이스바·Enter를 게임 화면이 바로 받게
        }
    }

    private void addScreen(Screen screen, JPanel panel) {
        root.add(panel, screen.name());
    }
}