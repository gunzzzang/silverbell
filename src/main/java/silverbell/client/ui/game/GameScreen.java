package silverbell.client.ui.game;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import silverbell.client.net.MessageRouter;

/**
 * 게임 화면(UI 명세서 S4~S9). 영역 패널을 만들어 <b>배치만</b> 하고, 내용은 각 영역 클래스가 맡는다.
 * 두 라인이 같은 파일을 동시에 고치지 않게 영역마다 클래스를 나눴다(#7).
 *
 * <pre>
 * ┌─────────────── 문제 QuestionPanel (A) ───────────────┐
 * │ 문제 n · 유형 · 점수 / 문제 / 힌트 / 시간 바            │
 * ├──────────────────────────────┬───────────────────────┤
 * │ 벨 BellPanel (B)             │ 벨 순서 QueuePanel (B)  │
 * │                              ├───────────────────────┤
 * │ 답 입력 AnswerPanel (B)       │ 점수판 ScorePanel (A)   │
 * │ 보기 1~4 또는 입력칸          │                       │
 * ├──────────────────────────────┴───────────────────────┤
 * │ 채팅 ChatPanel (A)                                     │
 * └──────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>배치는 UI 명세서 5절 기준(채팅 아래, U1 배치). 창 크기를 바꿀 수 있어서 수업의 {@code setLayout(null)} 대신
 * {@code BorderLayout}으로 영역을 나눴다(CLAUDE.md 3-4 「단순한 영역은 BorderLayout」).
 *
 * <p><b>키 입력은 이 화면이 받는다</b>(수업 {@code KeyInputExample}·{@code FocusExample} 방식).
 * 채팅 입력칸이 닫혀 있을 때는 이 패널이 포커스를 가지고, 열리면 입력칸이 포커스를 가져가서
 * 스페이스바·숫자키가 글자로 들어간다. 그래서 「입력칸이 닫혀 있을 때만 벨」(PRD B1)이 따로 검사 없이 지켜진다.
 * 입력칸을 닫을 때는 {@link #takeKeyFocus()}로 포커스를 돌려받는다.
 * 영역 안의 버튼은 {@code setFocusable(false)}로 둔다. 버튼이 포커스를 가지면 스페이스바가 그 버튼을 눌러 버린다.
 *
 * <p>배치와 키 연결은 두 라인이 같이 쓰는 부분이라, 고칠 때는 상대에게 먼저 알린다.
 */
public final class GameScreen extends JPanel {

    private static final int QUESTION_HEIGHT = 150;
    /** 보기 버튼 2×2(높이 48, UI 명세서 5절) + 안내 한 줄이 들어갈 높이 */
    private static final int ANSWER_HEIGHT = 150;
    private static final int SIDE_WIDTH = 240;
    private static final int CHAT_HEIGHT = 150;
    /** UI 명세서 5절: 패널 바깥 여백 16, 요소 사이 8 */
    private static final int OUTER_GAP = 16;
    private static final int INNER_GAP = 8;

    private final QuestionPanel questionPanel = new QuestionPanel();
    private final BellPanel bellPanel = new BellPanel();
    private final AnswerPanel answerPanel = new AnswerPanel();
    private final QueuePanel queuePanel = new QueuePanel();
    private final ScorePanel scorePanel = new ScorePanel();
    private final ChatPanel chatPanel = new ChatPanel();

    /**
     * 영역을 배치하고, 각 영역이 받을 메시지를 {@code router}에 등록하게 한다.
     * {@code ServerConnection.connect} 전에 만들어야 처음 오는 메시지를 놓치지 않는다.
     */
    public GameScreen(MessageRouter router) {
        super(new BorderLayout(INNER_GAP, INNER_GAP));
        setBorder(BorderFactory.createEmptyBorder(OUTER_GAP, OUTER_GAP, OUTER_GAP, OUTER_GAP));

        questionPanel.setPreferredSize(new Dimension(0, QUESTION_HEIGHT));
        answerPanel.setPreferredSize(new Dimension(0, ANSWER_HEIGHT));
        chatPanel.setPreferredSize(new Dimension(0, CHAT_HEIGHT));

        JPanel center = new JPanel(new BorderLayout(INNER_GAP, INNER_GAP)); // 가운데: 벨 위, 답 입력 아래
        center.add(bellPanel, BorderLayout.CENTER);
        center.add(answerPanel, BorderLayout.SOUTH);

        JPanel side = new JPanel(new GridLayout(2, 1, INNER_GAP, INNER_GAP)); // 오른쪽: 벨 순서 위, 점수판 아래 반반
        side.setPreferredSize(new Dimension(SIDE_WIDTH, 0));
        side.add(queuePanel);
        side.add(scorePanel);

        add(questionPanel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
        add(chatPanel, BorderLayout.SOUTH);

        // 영역마다 자기 메시지 등록. 이후 이슈는 각 영역 클래스의 register 안만 고친다
        questionPanel.register(router);
        bellPanel.register(router);
        answerPanel.register(router);
        queuePanel.register(router);
        scorePanel.register(router);
        chatPanel.register(router);

        setFocusable(true); // 패널이 키 입력을 받을 수 있게
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) { // 채팅 입력칸이 닫혀 있을 때만 여기로 온다
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        bellPanel.ring();
                        break;
                    case KeyEvent.VK_ENTER:
                        chatPanel.openInput();
                        break;
                    case KeyEvent.VK_1:
                    case KeyEvent.VK_2:
                    case KeyEvent.VK_3:
                    case KeyEvent.VK_4:
                        answerPanel.chooseOption(e.getKeyCode() - KeyEvent.VK_0); // VK_1 - VK_0 = 1
                        break;
                    case KeyEvent.VK_NUMPAD1:
                    case KeyEvent.VK_NUMPAD2:
                    case KeyEvent.VK_NUMPAD3:
                    case KeyEvent.VK_NUMPAD4:
                        answerPanel.chooseOption(e.getKeyCode() - KeyEvent.VK_NUMPAD0); // 오른쪽 숫자 패드도 같게
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * 키 입력을 게임 화면으로 가져온다. 게임 화면으로 바뀔 때와 채팅 입력칸이 닫힐 때 부른다.
     * 화면이 막 바뀐 직후에는 포커스 요청이 실패할 수 있어서 수업 {@code FocusAdapterExample}처럼 {@code invokeLater}로 미룬다.
     */
    public void takeKeyFocus() {
        SwingUtilities.invokeLater(() -> requestFocusInWindow());
    }
}
