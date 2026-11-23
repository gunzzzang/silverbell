package silverbell.client.ui.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import silverbell.client.net.MessageRouter;
import silverbell.common.Message;
import silverbell.common.MessageType;

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
 * <p>지금은 차례 안내(#14)만 있다. 보기 버튼·답 입력칸은 #16, 꾸미기는 M4(#27).
 */
class AnswerPanel extends JPanel {

    /** UI 명세서 5절: 내 차례 강조는 노란색 */
    private static final Color MY_TURN_COLOR = new Color(255, 236, 140);

    private final JLabel lblTurn = new JLabel(" ", SwingConstants.CENTER);
    private final Color normalColor;
    private String myNickname = "";

    AnswerPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("답 입력 (라인 B)"));
        normalColor = getBackground();

        lblTurn.setFont(lblTurn.getFont().deriveFont(Font.BOLD, 16f));
        add(lblTurn, BorderLayout.NORTH);
        add(new JLabel("답 입력 · 내 차례에만 켜짐", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * QUESTION  4지선다면 보기 버튼 4개, 단답형이면 입력칸 (둘 다 꺼진 채로) (#16)
     * TURN      내 차례면 「내 차례!」, 아니면 「○○ 님 답변 중…」 (#14) · 내 차례면 보기·입력칸 켜고 커서 (#16)
     * RESULT    보기·입력칸 끄기 (#16)
     * REVEAL    차례 안내 지우기 (#14)
     * </pre>
     */
    void register(MessageRouter router) {
        router.on(MessageType.TURN, message -> showTurn(message));
        router.on(MessageType.QUESTION, message -> clearTurn());
        router.on(MessageType.REVEAL, message -> clearTurn());
    }

    /** 내 닉네임. 지금 차례가 나인지 알 때 쓴다. */
    void setMyNickname(String nickname) {
        this.myNickname = nickname;
    }

    /** {@code TURN|주성;10000} → 내가 주성이면 「내 차례!」, 아니면 「주성 님 답변 중…」. {@code TURN|}이면 지운다. */
    private void showTurn(Message message) {
        String who = message.values(2)[0]; // 차례 없음(TURN|)이면 빈 문자열
        if (who.isEmpty()) {
            clearTurn();
            return;
        }
        if (who.equals(myNickname)) {
            lblTurn.setText("내 차례! 시간 안에 답하세요");
            setBackground(MY_TURN_COLOR);
        } else {
            lblTurn.setText(who + " 님 답변 중…");
            setBackground(normalColor);
        }
    }

    private void clearTurn() {
        lblTurn.setText(" ");
        setBackground(normalColor);
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
