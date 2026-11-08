package silverbell.client.ui;

import java.awt.CardLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 게임 창 하나. {@link CardLayout}으로 타이틀·대기실·게임·결과 화면을 겹쳐 두고 하나만 보여준다.
 * 화면마다 창을 새로 띄우지 않아서 위치·크기가 유지되고 전환이 자연스럽다.
 *
 * <p>할 일: 각 화면 패널을 UI 명세서대로 채운다 (M1 틀 → M4 꾸미기).
 */
public final class MainFrame extends JFrame {

    private static final int WIDTH = 960;
    private static final int HEIGHT = 640;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    public MainFrame() {
        super("실버벨");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);

        addScreen(Screen.TITLE, new PlaceholderScreen("타이틀 · 닉네임·서버 주소 입력 (A1)"));
        addScreen(Screen.LOBBY, new PlaceholderScreen("대기실 · 참가자 목록·채팅·준비 (A2, A3, B1)"));
        addScreen(Screen.GAME, new PlaceholderScreen("게임 · 문제·벨·답안·점수판·채팅 (C, D, E, F)"));
        addScreen(Screen.RESULT, new PlaceholderScreen("결과 · 최종 순위 (F3)"));

        setContentPane(root);
        show(Screen.TITLE);
    }

    /** 보여줄 화면을 바꾼다. EDT에서 불러야 한다. */
    public void show(Screen screen) {
        cards.show(root, screen.name());
    }

    private void addScreen(Screen screen, JPanel panel) {
        root.add(panel, screen.name());
    }
}
