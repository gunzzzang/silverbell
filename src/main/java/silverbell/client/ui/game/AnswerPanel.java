package silverbell.client.ui.game;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import silverbell.client.net.MessageRouter;

/**
 * 답 입력 영역: 「내 차례!」·「○○ 님 답변 중…」 안내, 답변 시간, 4지선다 보기 버튼 4개(2×2, 앞에 1~4) 또는 단답형 답 입력칸.
 * UI 명세서 S6 게임 · 내 차례 (D3 순서 표시, D4 답변 시간, E1 답안 입력).
 *
 * <p>답을 보내는 방법(보기 클릭·숫자키·입력칸 Enter)을 이 영역 한 곳에 모았다. 보기 버튼도 여기 있어서
 * 「내 차례에만 켜기」와 「누르면 {@code ANSWER}」를 한 클래스에서 처리한다.
 * 보기 버튼은 {@code setFocusable(false)}로 만든다(스페이스바가 버튼을 누르지 않게, {@link GameScreen} 참고).
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
     * QUESTION  4지선다면 보기 버튼 4개, 단답형이면 입력칸 (둘 다 꺼진 채로) (#16)
     * TURN      내 차례면 「내 차례!」, 아니면 「○○ 님 답변 중…」 (#14) · 내 차례면 보기·입력칸 켜고 커서 (#16)
     * RESULT    보기·입력칸 끄기 (#16)
     * </pre>
     */
    void register(MessageRouter router) {
        // 각 이슈에서 위 표의 종류를 router.on(...)으로 등록한다
    }

    /**
     * 4지선다에서 답을 고른다. 숫자키 1~4({@link GameScreen})와 보기 버튼 클릭이 둘 다 이 메서드를 부른다.
     * 4지선다이고 내 차례일 때만 그 번호로 답을 보낸다.
     *
     * @param number 보기 번호 1~4
     */
    void chooseOption(int number) {
        // #16: 내 차례면 ANSWER|번호 보내기
    }
}
