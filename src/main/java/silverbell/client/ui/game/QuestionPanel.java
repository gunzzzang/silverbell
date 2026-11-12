package silverbell.client.ui.game;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import silverbell.client.net.MessageRouter;

/**
 * 문제 영역: 「문제 n · 유형 · 난이도 점수」, 문제 글, 단답형 초성 힌트 칸, 남은 시간 바.
 * 4지선다 보기 버튼은 답을 보내는 곳이라 답 입력 영역({@link AnswerPanel}, 라인 B)에 있다.
 * UI 명세서 S4 게임 · 문제 제시 위쪽 (C1 문제 제시, C2 문제 타이머, C4 초성 힌트).
 *
 * <p><b>담당: 라인 A</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 *
 * <p>지금은 자리만 잡은 빈 패널이다(#7). 꾸미기는 M4(#26).
 */
class QuestionPanel extends JPanel {

    QuestionPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("문제 (라인 A)"));
        add(new JLabel("문제", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * COUNTDOWN  가운데 3, 2, 1 (#19)
     * QUESTION   「문제 n · 유형 · 난이도 점수」, 문제, 시간 바 시작 (#11)
     * HINT       힌트 칸에 초성 힌트, 바뀐 점수 (#18)
     * REVEAL     정답 초록색 표시 (#11)
     * </pre>
     */
    void register(MessageRouter router) {
        // 각 이슈에서 위 표의 종류를 router.on(...)으로 등록한다
    }
}
