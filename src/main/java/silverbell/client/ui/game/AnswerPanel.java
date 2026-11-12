package silverbell.client.ui.game;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import silverbell.client.net.MessageRouter;

/**
 * 답 입력 영역: 「내 차례!」·「○○ 님 답변 중…」 안내, 답변 시간, 단답형 답 입력칸.
 * UI 명세서 S6 게임 · 내 차례 (D3 순서 표시, D4 답변 시간, E1 답안 입력).
 *
 * <p><b>담당: 라인 B</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 *
 * <p>지금은 자리만 잡은 빈 패널이다(#7). 꾸미기는 M4(#27).
 */
class AnswerPanel extends JPanel {

    AnswerPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("답 입력 (라인 B)"));
        add(new JLabel("답 입력 · 내 차례에만 켜짐", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * TURN    내 차례면 「내 차례!」, 아니면 「○○ 님 답변 중…」 (#14) · 내 차례면 입력 켜고 커서 (#16)
     * RESULT  입력 끄기 (#16)
     * </pre>
     */
    void register(MessageRouter router) {
        // 각 이슈에서 위 표의 종류를 router.on(...)으로 등록한다
    }

    /**
     * 숫자키 1~4를 누르면 {@link GameScreen}이 부른다. 4지선다이고 내 차례일 때만 그 번호로 답을 보낸다.
     *
     * @param number 보기 번호 1~4
     */
    void chooseOption(int number) {
        // #16: 내 차례면 ANSWER|번호 보내기
    }
}
