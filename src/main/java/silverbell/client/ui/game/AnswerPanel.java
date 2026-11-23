package silverbell.client.ui.game;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import silverbell.client.net.MessageRouter;
import silverbell.client.net.MessageSender;
import silverbell.common.AnswerResult;
import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 답 입력 영역: 「내 차례!」·「○○ 님 답변 중…」·판정 안내, 4지선다 보기 버튼 4개(2×2, 앞에 1~4) 또는 단답형 답 입력칸.
 * UI 명세서 S6 게임 · 내 차례 (D3 순서 표시, E1 답안 입력, E3 판정 알림).
 *
 * <p>답을 보내는 방법(보기 클릭·숫자키·입력칸 Enter)을 이 영역 한 곳에 모았다. 보기 버튼도 여기 있어서
 * 「내 차례에만 켜기」와 「누르면 {@code ANSWER}」를 한 클래스에서 처리한다.
 * 보기 버튼은 {@code setFocusable(false)}로 만든다(스페이스바가 버튼을 누르지 않게, {@link GameScreen} 참고).
 * 단답형 입력칸은 내 차례에만 켜지고 커서가 간다(UI 명세서 2.1 규칙 6). 차례가 끝나면 키 입력을 게임 화면에 돌려준다.
 *
 * <p><b>담당: 라인 B</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 *
 * <p>차례 안내(#14), 보기 버튼·답 입력칸·판정 안내(#16). 답변 시간 카운트다운은 #21, 꾸미기는 M4(#27).
 */
class AnswerPanel extends JPanel {

    /** UI 명세서 5절: 내 차례 강조는 노란색, 정답·오답은 초록·빨강 */
    private static final Color MY_TURN_COLOR = new Color(255, 236, 140);
    private static final Color CORRECT_COLOR = new Color(190, 240, 190);
    private static final Color WRONG_COLOR = new Color(250, 195, 195);
    private static final float TURN_FONT_SIZE = 16f;
    /** {@code QUESTION}의 유형 값. 서버 {@code AnswerCheckerSelector}와 같은 이름 */
    private static final String MULTIPLE_CHOICE = "4지선다";
    /** 입력 자리 카드 이름 (수업 밖 {@code CardLayout}은 {@code MainFrame}과 같은 방식). 게임 화면은 {@code QUESTION}을 받아야 보여서, 그때 둘 중 하나로 바뀐다 */
    private static final String CARD_CHOICE = "choice";
    private static final String CARD_SHORT = "short";

    private final JLabel lblTurn = new JLabel(" ", SwingConstants.CENTER);
    private final CardLayout inputCards = new CardLayout();
    private final JPanel inputPanel = new JPanel(inputCards);
    private final JButton[] btnChoices = new JButton[4];
    private final JTextField txtAnswer = new JTextField();
    private final MessageSender sender;
    /** 차례가 끝나면 키 입력을 돌려줄 게임 화면 ({@link ChatPanel}처럼 생성자로 받는다) */
    private final GameScreen screen;
    private final Color normalColor;
    private String myNickname = "";
    /** 지금 문제가 4지선다인지. {@code QUESTION}을 받을 때 정해진다 */
    private boolean multipleChoice = false;
    /** 이번 문제에 판정 안내를 띄웠는지. 정답 공개 때 그 안내를 지우지 않으려고 쓴다 */
    private boolean showingResult = false;

    AnswerPanel(MessageSender sender, GameScreen screen) {
        super(new BorderLayout(8, 8));
        this.sender = sender;
        this.screen = screen;
        setBorder(BorderFactory.createTitledBorder("답 입력 (라인 B)"));
        normalColor = getBackground();

        lblTurn.setFont(lblTurn.getFont().deriveFont(Font.BOLD, TURN_FONT_SIZE));
        add(lblTurn, BorderLayout.NORTH);

        JPanel choicePanel = new JPanel(new GridLayout(2, 2, 8, 8)); // 보기 2×2 (UI 명세서 5절)
        for (int i = 0; i < btnChoices.length; i++) {
            btnChoices[i] = new JButton((i + 1) + ".");
            btnChoices[i].setFocusable(false); // 버튼이 포커스를 가지면 스페이스바가 벨 대신 이 버튼을 누른다
            btnChoices[i].addActionListener(new ChoiceAction(i + 1));
            choicePanel.add(btnChoices[i]);
        }
        txtAnswer.addActionListener(new SubmitAction()); // 입력칸 Enter = 답 보내기
        JPanel shortPanel = new JPanel(new BorderLayout(4, 4)); // 입력칸이 세로로 늘어나지 않게 위에 한 줄로
        shortPanel.add(txtAnswer, BorderLayout.NORTH);
        shortPanel.add(new JLabel("내 차례가 되면 답을 쓰고 Enter", SwingConstants.CENTER), BorderLayout.CENTER);

        inputPanel.add(choicePanel, CARD_CHOICE);
        inputPanel.add(shortPanel, CARD_SHORT);
        add(inputPanel, BorderLayout.CENTER);
        turnOffInput();
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * QUESTION  4지선다면 보기 버튼 4개, 단답형이면 입력칸 (둘 다 꺼진 채로) (#16)
     * TURN      내 차례면 「내 차례!」 + 보기·입력칸 켜고 커서, 아니면 「○○ 님 답변 중…」 (#14·#16)
     * RESULT    보기·입력칸 끄고 「○○ 님 정답! +2점」·「○○ 님 오답! -1점」 (#16)
     * REVEAL    보기·입력칸 끄기, 판정 안내가 없으면 차례 안내 지우기 (#14·#16)
     * </pre>
     */
    void register(MessageRouter router) {
        router.on(MessageType.QUESTION, message -> showQuestion(message));
        router.on(MessageType.TURN, message -> showTurn(message));
        router.on(MessageType.RESULT, message -> showResult(message));
        router.on(MessageType.REVEAL, message -> onReveal());
    }

    /** 내 닉네임. 지금 차례가 나인지 알 때 쓴다. */
    void setMyNickname(String nickname) {
        this.myNickname = nickname;
    }

    /**
     * {@code QUESTION|번호;유형;난이도;제한 시간;보기1;보기2;보기3;보기4;문제} → 4지선다면 「1. 보기」 버튼 4개, 단답형이면 입력칸.
     * 차례가 생기기 전이라 꺼진 채로 그린다.
     */
    private void showQuestion(Message message) {
        String[] values = message.values(9);
        if (values.length < 9) {
            return;
        }
        multipleChoice = MULTIPLE_CHOICE.equals(values[1]);
        if (multipleChoice) {
            for (int i = 0; i < btnChoices.length; i++) {
                btnChoices[i].setText((i + 1) + ". " + values[4 + i]); // 보기1~4는 4~7번째 값
            }
            inputCards.show(inputPanel, CARD_CHOICE);
        } else {
            txtAnswer.setText("");
            inputCards.show(inputPanel, CARD_SHORT);
        }
        turnOffInput();
        clearTurn();
    }

    /** {@code TURN|주성;10000} → 내가 주성이면 「내 차례!」와 입력 켜기, 아니면 「주성 님 답변 중…」. {@code TURN|}이면 지운다. */
    private void showTurn(Message message) {
        String who = message.values(2)[0]; // 차례 없음(TURN|)이면 빈 문자열
        showingResult = false; // 차례가 바뀌면 앞사람 판정 안내는 끝
        if (who.isEmpty()) {
            turnOffInput();
            clearTurn();
            return;
        }
        if (who.equals(myNickname)) {
            lblTurn.setText("내 차례! 시간 안에 답하세요");
            setBackground(MY_TURN_COLOR);
            turnOnInput();
        } else {
            lblTurn.setText(who + " 님 답변 중…");
            setBackground(normalColor);
            turnOffInput();
        }
    }

    /** {@code RESULT|이건;정답;2} → 「이건 님 정답! +2점」(초록), {@code RESULT|주성;오답;-1} → 「주성 님 오답! -1점」(빨강). */
    private void showResult(Message message) {
        String[] values = message.values(3);
        if (values.length < 3) {
            return;
        }
        turnOffInput();
        showingResult = true;
        if (AnswerResult.CORRECT.equals(values[1])) {
            lblTurn.setText(values[0] + " 님 정답! +" + values[2] + "점");
            setBackground(CORRECT_COLOR);
        } else {
            lblTurn.setText(values[0] + " 님 " + values[1] + "! " + values[2] + "점"); // 오답 (#21의 시간 초과도 같은 모양)
            setBackground(WRONG_COLOR);
        }
    }

    /** 정답 공개. 입력을 끄고, 맞힌 사람 안내는 남긴다(정답이면 RESULT 바로 뒤에 온다). */
    private void onReveal() {
        turnOffInput();
        if (!showingResult) {
            clearTurn();
        }
    }

    private void clearTurn() {
        lblTurn.setText(" ");
        setBackground(normalColor);
    }

    /** 내 차례에 보기 버튼이나 입력칸을 켠다. 단답형은 바로 칠 수 있게 커서를 옮긴다. */
    private void turnOnInput() {
        if (multipleChoice) {
            for (int i = 0; i < btnChoices.length; i++) {
                btnChoices[i].setEnabled(true);
            }
            return;
        }
        txtAnswer.setEnabled(true);
        SwingUtilities.invokeLater(() -> txtAnswer.requestFocusInWindow()); // 막 켜진 직후라 미뤄서 포커스 요청 (수업 FocusAdapterExample)
    }

    /** 보기 버튼과 입력칸을 끈다. 입력칸에 커서가 있었으면 키 입력을 게임 화면에 돌려준다(스페이스바가 다시 벨). */
    private void turnOffInput() {
        boolean hadFocus = txtAnswer.isFocusOwner(); // 끄기 전에 확인 (꺼지면 포커스가 엉뚱한 곳으로 갈 수 있다)
        for (int i = 0; i < btnChoices.length; i++) {
            btnChoices[i].setEnabled(false);
        }
        txtAnswer.setEnabled(false);
        if (hadFocus) {
            screen.takeKeyFocus();
        }
    }

    /**
     * 4지선다에서 답을 고른다. 숫자키 1~4({@link GameScreen})와 보기 버튼 클릭이 둘 다 이 메서드를 부른다.
     * 4지선다이고 보기가 켜져 있을 때(내 차례)만 그 번호로 답을 보낸다.
     *
     * @param number 보기 번호 1~4
     */
    void chooseOption(int number) {
        if (!multipleChoice || !btnChoices[0].isEnabled()) {
            return;
        }
        sendAnswer(String.valueOf(number));
    }

    /** 답을 한 번만 보내고 입력을 끈다. 판정은 서버가 {@code RESULT}로 알려준다. 서버도 차례가 아니면 무시한다(N7). */
    private void sendAnswer(String answer) {
        turnOffInput(); // 연타·Enter 두 번에도 한 번만 보낸다
        lblTurn.setText("답을 보냈어요…");
        sender.send(Message.of(MessageType.ANSWER, answer));
    }

    /** 보기 버튼 클릭. 숫자키와 같은 동작이라 {@link #chooseOption(int)}만 부른다. */
    private class ChoiceAction implements ActionListener {
        private final int number;

        ChoiceAction(int number) {
            this.number = number;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            chooseOption(number);
        }
    }

    /** 단답형 입력칸 Enter: 쓴 게 있으면 답을 보낸다. 빈 칸이면 그대로 둔다. */
    private class SubmitAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String text = txtAnswer.getText();
            if (text.isBlank()) {
                return;
            }
            sendAnswer(text); // 앞뒤 공백·띄어쓰기는 서버 채점기가 무시한다(E2)
        }
    }
}
