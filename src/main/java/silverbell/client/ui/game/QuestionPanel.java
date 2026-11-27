package silverbell.client.ui.game;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import silverbell.client.net.MessageRouter;
import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 문제 영역: 「문제 n · 유형 · 난이도 점수」, 문제 글, 단답형 초성 힌트 칸, 남은 시간 바.
 * 4지선다 보기 버튼은 답을 보내는 곳이라 답 입력 영역({@link AnswerPanel}, 라인 B)에 있다.
 * UI 명세서 S4 게임 · 문제 제시 위쪽 (C1 문제 제시, C2 문제 타이머, C4 초성 힌트).
 *
 * <p><b>담당: 라인 A</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 *
 * <p>M2(#11)에서 최소 Swing 컴포넌트로 동작을 구현했다. 꾸미기는 M4(#26).
 */
class QuestionPanel extends JPanel {

    private static final int TIMER_TICK_MS = 100;
    private static final Color ACTIVE_BAR_COLOR = new Color(66, 133, 244);
    private static final Color PAUSED_BAR_COLOR = Color.GRAY;
    private static final Color HINT_TEXT_COLOR = new Color(26, 115, 232);
    private static final Color REVEAL_TEXT_COLOR = new Color(27, 138, 62);

    private final JLabel infoLabel = new JLabel("대기 중...", SwingConstants.LEFT);
    private final JTextArea questionArea = new JTextArea("문제");
    private final JLabel hintLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel revealLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JProgressBar timeBar = new JProgressBar();
    private final JLabel timeTextLabel = new JLabel("0.0초", SwingConstants.RIGHT);

    private Timer countdownTimer;
    private int totalMs;
    private int remainingMs;
    private long deadlineMs;

    private String currentQuestionNumber = "";
    private String currentQuestionType = "";
    private String currentBaseDifficulty = "";

    QuestionPanel() {
        super(new BorderLayout(8, 4));
        setBorder(BorderFactory.createTitledBorder("문제 (라인 A)"));

        // 상단: 「문제 n · 유형 · 난이도 점수」
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(infoLabel, BorderLayout.NORTH);

        // 중단: 문제 영역: 「문제 n · 유형 · 난이도 점수」, 문제 글, 단답형 초성 힌트 칸, 남은 시간 바
        questionArea.setEditable(false);
        questionArea.setFocusable(false);
        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setBackground(Color.WHITE);
        questionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        questionArea.setFont(questionArea.getFont().deriveFont(16f));
        add(questionArea, BorderLayout.CENTER);

        // 하단: 초성 힌트 라벨 + 정답 공개 라벨 + 남은 시간 바
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.BOLD, 15f));
        hintLabel.setForeground(HINT_TEXT_COLOR);

        revealLabel.setFont(revealLabel.getFont().deriveFont(Font.BOLD, 15f));
        revealLabel.setForeground(REVEAL_TEXT_COLOR);

        timeBar.setForeground(ACTIVE_BAR_COLOR);
        JPanel timerRow = new JPanel(new BorderLayout(8, 0));
        timerRow.add(timeBar, BorderLayout.CENTER);
        timerRow.add(timeTextLabel, BorderLayout.EAST);

        JPanel southPanel = new JPanel(new BorderLayout(4, 2));
        southPanel.add(hintLabel, BorderLayout.NORTH);
        southPanel.add(revealLabel, BorderLayout.CENTER);
        southPanel.add(timerRow, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        initTimer();
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * COUNTDOWN  가운데 3, 2, 1 (#19)
     * QUESTION   「문제 n · 유형 · 난이도 점수」, 문제, 시간 바 시작 (#11)
     * HINT       힌트 칸에 초성 힌트, 바뀐 점수 (#18)
     * REVEAL     정답 초록색 표시 (#11)
     * TURN       누가 답하는 동안 시간 바 멈춤(회색), 차례가 없어지면 다시 흐름 (#11)
     * </pre>
     */
    void register(MessageRouter router) {
        router.on(MessageType.QUESTION, this::onQuestion);
        router.on(MessageType.HINT, this::onHint);
        router.on(MessageType.REVEAL, this::onReveal);
        router.on(MessageType.TURN, this::onTurn);
    }

    private void initTimer() {
        countdownTimer = new Timer(TIMER_TICK_MS, e -> {
            remainingMs = (int) (deadlineMs - System.currentTimeMillis());
            if (remainingMs <= 0) {
                remainingMs = 0;
                countdownTimer.stop();
            }
            refreshTimerView();
        });
    }

    /**
     * QUESTION|번호;유형;난이도;제한 시간(ms);보기1;보기2;보기3;보기4;문제
     */
    private void onQuestion(Message message) {
        String[] values = message.values(9);
        if (values.length < 9) {
            return;
        }
        this.currentQuestionNumber = values[0];
        this.currentQuestionType = values[1];
        this.currentBaseDifficulty = values[2];
        int limitMs = Integer.parseInt(values[3]);
        String prompt = values[8];

        infoLabel.setText("문제 " + currentQuestionNumber + " · " + currentQuestionType + " · 난이도 " + currentBaseDifficulty + "점");
        questionArea.setText(prompt);
        hintLabel.setText(" ");
        revealLabel.setText(" ");

        this.totalMs = limitMs;
        this.remainingMs = limitMs;
        this.deadlineMs = System.currentTimeMillis() + limitMs;
        timeBar.setMinimum(0);
        timeBar.setMaximum(totalMs);
        timeBar.setValue(totalMs);
        timeBar.setForeground(ACTIVE_BAR_COLOR);
        refreshTimerView();

        countdownTimer.restart();
    }

    /**
     * HINT|힌트 단계;초성 힌트;바뀐 점수
     */
    private void onHint(Message message) {
        String[] values = message.values(3);
        if (values.length < 3) {
            return;
        }
        String stage = values[0];
        String hintText = values[1];
        String reducedScore = values[2];

        hintLabel.setText("[" + stage + "차 힌트] " + hintText);
        infoLabel.setText(
                "문제 " + currentQuestionNumber + " · " + currentQuestionType
                        + " · 현재 " + reducedScore + "점 (기본 " + currentBaseDifficulty + "점)"
        );
    }

    /**
     * REVEAL|정답
     */
    private void onReveal(Message message) {
        countdownTimer.stop();
        remainingMs = 0;
        refreshTimerView();
        revealLabel.setText("정답: " + message.body());
    }

    /**
     * TURN|지금 차례인 사람;답변 시간
     * 누가 답하는 동안 시간 바 멈춤(회색), 차례가 없어지면(빈 값) 다시 흐름
     */
    private void onTurn(Message message) {
        String[] values = message.values(2);
        String turnUser = values.length > 0 ? values[0].strip() : "";
        boolean hasTurn = !turnUser.isEmpty();

        if (hasTurn) {
            if (countdownTimer.isRunning()) {
                remainingMs = (int) Math.max(0, deadlineMs - System.currentTimeMillis());
                countdownTimer.stop();
            }
            timeBar.setForeground(PAUSED_BAR_COLOR);
            refreshTimerView();
        } else {
            timeBar.setForeground(ACTIVE_BAR_COLOR);
            if (remainingMs > 0) {
                deadlineMs = System.currentTimeMillis() + remainingMs;
                countdownTimer.start();
            }
        }
    }

    private void refreshTimerView() {
        timeBar.setValue(remainingMs);
        timeTextLabel.setText(String.format("%.1f초", remainingMs / 1000.0));
        if (remainingMs <= 0) {
            // Windows Look & Feel에서 value=0일 때 남는 1~2px 파란 잔상 제거
            timeBar.setForeground(timeBar.getBackground());
        }
    }
}