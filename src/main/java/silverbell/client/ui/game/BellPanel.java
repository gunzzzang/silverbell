package silverbell.client.ui.game;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import silverbell.client.net.MessageRouter;

/**
 * 벨 영역: 큰 벨 버튼, 「스페이스바로 벨!」 안내, 「내 순서 n번」·「이 문제는 다시 못 울려요」.
 * UI 명세서 S4~S8 게임 화면 가운데 (D1 벨 누르기, D6 재도전 금지).
 *
 * <p><b>담당: 라인 B</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 *
 * <p>지금은 자리만 잡은 빈 패널이다(#7). 꾸미기는 M4(#27).
 */
class BellPanel extends JPanel {

    BellPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("벨 (라인 B)"));
        add(new JLabel("벨", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * QUESTION  벨 켜기 (#14)
     * RESULT    정답이면 벨 끄기 (#16) · 내가 틀렸으면 회색 + 「이 문제는 다시 못 울려요」 (#21)
     * REVEAL    벨 끄기 (#14)
     * </pre>
     */
    void register(MessageRouter router) {
        // 각 이슈에서 위 표의 종류를 router.on(...)으로 등록한다
    }

    /** 벨을 울린다. 화면 벨 버튼과 스페이스바({@link GameScreen}) 둘 다 이 메서드를 부른다. */
    void ring() {
        // #14: BUZZ를 보내고 벨을 끈다
    }
}
