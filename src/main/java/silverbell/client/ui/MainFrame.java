package silverbell.client.ui;

import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import silverbell.client.net.MessageRouter;
import silverbell.client.net.ServerConnection;
import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 게임 창 하나. {@link CardLayout}으로 타이틀·대기실·게임·결과 화면을 겹쳐 두고 하나만 보여준다.
 * 화면마다 창을 새로 띄우지 않아서 위치·크기가 유지되고 전환이 자연스럽다.
 *
 * <p>할 일: 각 화면 패널을 UI 명세서대로 채운다 (#6 타이틀·대기실, #7 게임 화면 틀, #20 결과 화면 → #26·#27 꾸미기).
 */
public final class MainFrame extends JFrame {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 640;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final MessageRouter router = new MessageRouter();
    private final TitlePanel titlePanel;
    private final LobbyPanel lobbyPanel;
    private ServerConnection connection;

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
        addScreen(Screen.GAME, new PlaceholderScreen("게임 · 문제·벨·답안·점수판·채팅 (C, D, E, F)"));
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
            titlePanel.onJoinSuccess();
            show(Screen.LOBBY);
        });

        router.on(MessageType.JOIN_FAIL, message -> titlePanel.onJoinFailed(message.body()));

        router.on(MessageType.USERS, lobbyPanel::updateUsers);
        router.on(MessageType.CHAT, lobbyPanel::appendChat);
        router.on(MessageType.SYSTEM, lobbyPanel::appendSystem);

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

    /** 보여줄 화면을 바꾼다. EDT에서 불러야 한다. */
    public void show(Screen screen) {
        cards.show(root, screen.name());
    }

    private void addScreen(Screen screen, JPanel panel) {
        root.add(panel, screen.name());
    }
}