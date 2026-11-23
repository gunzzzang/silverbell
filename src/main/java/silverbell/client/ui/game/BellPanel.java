package silverbell.client.ui.game;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import silverbell.client.net.MessageRouter;
import silverbell.client.net.MessageSender;
import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 벨 영역: 큰 벨 버튼, 「스페이스바로 벨!」 안내, 「내 순서 n번」·「이 문제는 다시 못 울려요」.
 * UI 명세서 S4~S8 게임 화면 가운데 (D1 벨 누르기, D6 재도전 금지).
 *
 * <p><b>담당: 라인 B</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 *
 * <p>벨은 문제가 떠 있는 동안만 켜지고, 울리면 바로 꺼진다(UI 명세서 2.3 벨). 줄 순서는 서버가 정하므로
 * 화면은 서버가 보낸 {@code QUEUE}에서 내 자리만 찾아 보여준다. 최소 Swing 컴포넌트로 만들었고 꾸미기는 M4(#27).
 */
class BellPanel extends JPanel {

    private static final float BELL_FONT_SIZE = 28f;

    private final JButton btnBell = new JButton("벨!");
    private final JLabel lblInfo = new JLabel(" ", SwingConstants.CENTER);
    private final MessageSender sender;
    private String myNickname = "";

    BellPanel(MessageSender sender) {
        super(new BorderLayout(8, 8));
        this.sender = sender;
        setBorder(BorderFactory.createTitledBorder("벨 (라인 B)"));

        btnBell.setFont(btnBell.getFont().deriveFont(Font.BOLD, BELL_FONT_SIZE));
        btnBell.setFocusable(false); // 버튼이 포커스를 가지면 스페이스바가 벨 대신 이 버튼을 누른다 (GameScreen 참고)
        btnBell.setEnabled(false);   // 문제가 없을 때는 꺼 둔다
        btnBell.addActionListener(new BellAction());

        add(btnBell, BorderLayout.CENTER);
        add(lblInfo, BorderLayout.SOUTH);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * QUESTION  벨 켜기 (#14)
     * QUEUE     내가 줄에 있으면 「내 순서: n번」 (#14)
     * RESULT    정답이면 벨 끄기 (#16) · 내가 틀렸으면 회색 + 「이 문제는 다시 못 울려요」 (#21)
     * REVEAL    벨 끄기 (#14)
     * </pre>
     */
    void register(MessageRouter router) {
        router.on(MessageType.QUESTION, message -> turnOn());
        router.on(MessageType.QUEUE, message -> showMyPlace(message));
        router.on(MessageType.REVEAL, message -> turnOff());
    }

    /** 내 닉네임. 줄에서 내 자리를 찾을 때 쓴다. 입장할 때({@code JOIN_OK}) 정해진다. */
    void setMyNickname(String nickname) {
        this.myNickname = nickname;
    }

    /**
     * 벨을 울린다. 화면 벨 버튼과 스페이스바({@link GameScreen}) 둘 다 이 메서드를 부른다.
     * 벨이 꺼져 있으면(문제가 없거나 이미 울렸으면) 아무 일도 하지 않는다.
     */
    void ring() {
        if (!btnBell.isEnabled()) {
            return;
        }
        btnBell.setEnabled(false); // 연타해도 한 번만 보낸다. 서버도 한 번 더 막는다(N7)
        lblInfo.setText("딩!");
        sender.send(Message.of(MessageType.BUZZ));
    }

    private void turnOn() {
        btnBell.setEnabled(true);
        lblInfo.setText("스페이스바로 벨!");
    }

    private void turnOff() {
        btnBell.setEnabled(false);
        lblInfo.setText(" ");
    }

    /** {@code QUEUE|주성;이건} → 내가 2번째면 「내 순서: 2번」. 줄에 없으면 그대로 둔다. */
    private void showMyPlace(Message message) {
        String[] order = QueuePanel.readOrder(message);
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(myNickname)) {
                btnBell.setEnabled(false); // 줄에 섰으면 이 문제에서는 벨을 다시 누를 일이 없다
                lblInfo.setText("내 순서: " + (i + 1) + "번");
                return;
            }
        }
    }

    /** 벨 버튼 클릭. 스페이스바와 같은 동작이라 {@link #ring()}만 부른다. */
    private class BellAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ring();
        }
    }
}
