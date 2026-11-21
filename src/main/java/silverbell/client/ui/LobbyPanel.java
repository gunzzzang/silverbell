package silverbell.client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 대기실 화면 (S2). 참가자 목록과 채팅 영역을 제공한다.
 */
final class LobbyPanel extends JPanel {

    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JTextArea chatArea = new JTextArea();
    private final JTextField chatInput = new JTextField();
    private final JButton sendButton = new JButton("전송");
    /** #11 임시 게임 시작 버튼 (#19에서 준비 버튼·카운트다운으로 교체) */
    private final JButton startButton = new JButton("게임 시작");

    private final Consumer<Message> messageSender;
    private String myNickname;

    LobbyPanel(Consumer<Message> messageSender) {
        super(new BorderLayout(8, 8));
        this.messageSender = messageSender;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initLayout();
        initEvents();
    }

    private void initLayout() {
        // 좌측: 참가자 목록 + 방장 임시 시작 버튼
        JList<String> userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(BorderFactory.createTitledBorder("참가자 목록"));

        startButton.setEnabled(false); // 방장(HOST)으로 확인되면 활성화

        JPanel westPanel = new JPanel(new BorderLayout(0, 8));
        westPanel.setPreferredSize(new Dimension(220, 0));
        westPanel.add(userScroll, BorderLayout.CENTER);
        westPanel.add(startButton, BorderLayout.SOUTH);
        add(westPanel, BorderLayout.WEST);

        // 중앙: 채팅 영역
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("채팅"));
        add(chatScroll, BorderLayout.CENTER);

        // 하단: 입력창 + 전송 버튼
        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);
    }

    private void initEvents() {
        sendButton.addActionListener(e -> sendChat());
        chatInput.addActionListener(e -> sendChat());
        startButton.addActionListener(e -> {
            startButton.setEnabled(false); // 중복 클릭 방지
            messageSender.accept(Message.of(MessageType.START));
        });
    }

    void setMyNickname(String nickname) {
        this.myNickname = nickname;
    }

    private void sendChat() {
        String text = chatInput.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        messageSender.accept(Message.of(MessageType.CHAT, text));
        chatInput.setText("");
        chatInput.requestFocusInWindow();
    }

    /** USERS 메시지 수신 시 참가자 목록과 방장 시작 버튼 상태를 갱신한다. */
    void updateUsers(Message message) {
        userListModel.clear();
        String body = message.body();
        if (body.isEmpty()) {
            startButton.setEnabled(false);
            return;
        }
        boolean amIHost = false;
        for (String token : message.values(0)) {
            String[] parts = token.split(":");
            if (parts.length >= 2) {
                String nickname = parts[0];
                boolean isHost = "HOST".equals(parts[1]);
                userListModel.addElement(isHost ? "[방장] " + nickname : nickname);
                if (isHost && nickname.equals(myNickname)) {
                    amIHost = true;
                }
            }
        }
        startButton.setEnabled(amIHost);
    }

    /** CHAT 메시지 수신 시 채팅창에 추가한다. */
    void appendChat(Message message) {
        String[] parts = message.values(2);
        if (parts.length == 2) {
            appendLine(parts[0] + ": " + parts[1]);
        }
    }

    /** SYSTEM 메시지 수신 시 [시스템] 접두어를 붙여 출력한다. */
    void appendSystem(Message message) {
        appendLine("[시스템] " + message.body());
    }

    void reset() {
        userListModel.clear();
        chatArea.setText("");
        chatInput.setText("");
        startButton.setEnabled(false);
    }

    private void appendLine(String line) {
        chatArea.append(line + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
}