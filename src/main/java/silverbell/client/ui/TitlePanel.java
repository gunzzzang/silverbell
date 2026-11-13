package silverbell.client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import silverbell.client.net.MessageRouter;
import silverbell.client.net.ServerConnection;
import silverbell.common.GameConfig;
import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 타이틀 화면 (S1). 서버 주소와 닉네임을 입력받아 접속을 시도한다.
 */
final class TitlePanel extends JPanel {

    private final JTextField hostField = new JTextField("127.0.0.1", 15);
    private final JTextField portField = new JTextField(String.valueOf(GameConfig.DEFAULT_PORT), 6);
    private final JTextField nicknameField = new JTextField(15);
    private final JButton joinButton = new JButton("입장");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    private final MessageRouter router;
    private final Consumer<ServerConnection> onConnected;
    private ServerConnection pendingConnection;

    TitlePanel(MessageRouter router, Consumer<ServerConnection> onConnected) {
        super(new BorderLayout(10, 10));
        this.router = router;
        this.onConnected = onConnected;

        initLayout();
        initEvents();
        updateJoinButtonState();
    }

    private void initLayout() {
        JLabel titleLabel = new JLabel("실버벨 (Silverbell)", SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        form.add(new JLabel("서버 주소:"), gbc);
        gbc.gridx = 1;
        form.add(hostField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        form.add(new JLabel("포트 번호:"), gbc);
        gbc.gridx = 1;
        form.add(portField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        form.add(new JLabel("닉네임:"), gbc);
        gbc.gridx = 1;
        form.add(nicknameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        form.add(joinButton, gbc);

        add(form, BorderLayout.CENTER);

        statusLabel.setForeground(Color.RED);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void initEvents() {
        DocumentListener validator = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateJoinButtonState(); }
            @Override public void removeUpdate(DocumentEvent e) { updateJoinButtonState(); }
            @Override public void changedUpdate(DocumentEvent e) { updateJoinButtonState(); }
        };
        hostField.getDocument().addDocumentListener(validator);
        portField.getDocument().addDocumentListener(validator);
        nicknameField.getDocument().addDocumentListener(validator);

        joinButton.addActionListener(e -> attemptConnect());
        nicknameField.addActionListener(e -> {
            if (joinButton.isEnabled()) {
                attemptConnect();
            }
        });
    }

    private void updateJoinButtonState() {
        boolean hasBlank = hostField.getText().isBlank()
                || portField.getText().isBlank()
                || nicknameField.getText().isBlank();
        joinButton.setEnabled(!hasBlank);
    }

    private void attemptConnect() {
        String host = hostField.getText().strip();
        String nickname = nicknameField.getText().strip();
        int port;
        try {
            port = Integer.parseInt(portField.getText().strip());
        } catch (NumberFormatException e) {
            showError("포트 번호는 숫자여야 합니다.");
            return;
        }

        joinButton.setEnabled(false);
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setText("서버에 접속 중...");

        // EDT 밖(SwingWorker)에서 소켓 연결 수행
        new SwingWorker<ServerConnection, Void>() {
            @Override
            protected ServerConnection doInBackground() throws Exception {
                return ServerConnection.connect(host, port, router);
            }

            @Override
            protected void done() {
                try {
                    pendingConnection = get();
                    pendingConnection.send(Message.of(MessageType.JOIN, nickname));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("접속이 중단되었습니다.");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    String msg = (cause instanceof IOException)
                            ? "서버에 연결할 수 없습니다. 주소와 포트를 확인하세요."
                            : "접속 오류: " + cause.getMessage();
                    showError(msg);
                }
            }
        }.execute();
    }

    /** JOIN_OK 수신 시 MainFrame에서 호출한다. */
    void onJoinSuccess() {
        statusLabel.setText(" ");
        if (pendingConnection != null) {
            onConnected.accept(pendingConnection);
        }
    }

    /** JOIN_FAIL 수신 시 MainFrame에서 호출한다. */
    void onJoinFailed(String reason) {
        if (pendingConnection != null) {
            pendingConnection.close();
            pendingConnection = null;
        }
        showError(reason);
    }

    void showError(String message) {
        statusLabel.setForeground(Color.RED);
        statusLabel.setText(message);
        updateJoinButtonState();
    }
}