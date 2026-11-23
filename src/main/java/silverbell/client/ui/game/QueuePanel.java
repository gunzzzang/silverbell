package silverbell.client.ui.game;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import silverbell.client.net.MessageRouter;
import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 벨 순서 영역: 「1. 주성 (답변 중) / 2. 이건」처럼 줄 순서와 지금 차례.
 * UI 명세서 S5·S7 게임 화면 오른쪽 위 (D3 순서 표시).
 *
 * <p><b>담당: 라인 B</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 *
 * <p>줄 순서는 서버가 보낸 {@code QUEUE}를 그대로 보여준다. 맨 앞 사람이 지금 답변 차례다({@code MessageType.QUEUE}).
 * 최소 Swing 컴포넌트로 만들었고 꾸미기는 M4(#27).
 */
class QueuePanel extends JPanel {

    private final JTextArea textArea = new JTextArea();
    private String myNickname = "";

    QueuePanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("벨 순서 (라인 B)"));

        textArea.setEditable(false);
        textArea.setFocusable(false); // 키 입력은 게임 화면이 받는다 (GameScreen 참고)
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * QUEUE     줄 순서 다시 그리기 (#14)
     * QUESTION  새 문제 → 줄 비우기 (#14)
     * REVEAL    정답 공개 → 줄 비우기 (#14)
     * </pre>
     */
    void register(MessageRouter router) {
        router.on(MessageType.QUEUE, message -> showOrder(readOrder(message)));
        router.on(MessageType.QUESTION, message -> textArea.setText(""));
        router.on(MessageType.REVEAL, message -> textArea.setText(""));
    }

    /** 내 닉네임. 줄에서 내 이름 옆에 「(나)」를 붙일 때 쓴다. */
    void setMyNickname(String nickname) {
        this.myNickname = nickname;
    }

    /**
     * {@code QUEUE} 내용을 줄 순서 배열로 바꾼다. 줄이 비면({@code QUEUE|}) 빈 배열.
     * 벨 영역({@link BellPanel})도 내 순서를 찾을 때 같이 쓴다.
     */
    static String[] readOrder(Message message) {
        if (message.body().isEmpty()) {
            return new String[0];
        }
        return message.values(0); // 0 = 개수 제한 없이 전부 자르기 (목록 메시지)
    }

    /** 「1. 주성 (답변 중)」「2. 이건 (나)」처럼 한 줄에 한 명씩 그린다. */
    private void showOrder(String[] order) {
        textArea.setText("");
        for (int i = 0; i < order.length; i++) {
            String line = (i + 1) + ". " + order[i];
            if (order[i].equals(myNickname)) {
                line += " (나)";
            }
            if (i == 0) {
                line += " (답변 중)"; // 맨 앞 사람이 지금 차례
            }
            textArea.append(line + "\n"); // 수업 채팅창처럼 한 줄씩 붙인다
        }
    }
}
