package silverbell.client.ui.game;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import silverbell.client.net.MessageRouter;

/**
 * 점수판 영역: 플레이어별 점수를 높은 순으로.
 * UI 명세서 S4 게임 화면 오른쪽 아래 (F2 점수판).
 *
 * <p><b>담당: 라인 A</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 *
 * <p>지금은 자리만 잡은 빈 패널이다(#7). 꾸미기는 M4(#26).
 */
class ScorePanel extends JPanel {

    ScorePanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("점수판 (라인 A)"));
        add(new JLabel("점수판", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * SCORE  사람별 점수를 높은 순으로 다시 그리기 (#12)
     * </pre>
     */
    void register(MessageRouter router) {
        // 각 이슈에서 위 표의 종류를 router.on(...)으로 등록한다
    }
}
