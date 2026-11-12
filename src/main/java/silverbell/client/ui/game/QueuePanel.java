package silverbell.client.ui.game;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import silverbell.client.net.MessageRouter;

/**
 * 벨 순서 영역: 「1. 주성 (답변 중) / 2. 이건」처럼 줄 순서와 지금 차례.
 * UI 명세서 S5·S7 게임 화면 오른쪽 위 (D3 순서 표시).
 *
 * <p><b>담당: 라인 B</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 *
 * <p>지금은 자리만 잡은 빈 패널이다(#7). 꾸미기는 M4(#27).
 */
class QueuePanel extends JPanel {

    QueuePanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("벨 순서 (라인 B)"));
        add(new JLabel("벨 순서", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * QUEUE  줄 순서 다시 그리기 (#14)
     * TURN   답변 중인 사람 강조 (#14)
     * </pre>
     */
    void register(MessageRouter router) {
        // 각 이슈에서 위 표의 종류를 router.on(...)으로 등록한다
    }
}
