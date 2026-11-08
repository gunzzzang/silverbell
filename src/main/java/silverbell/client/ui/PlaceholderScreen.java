package silverbell.client.ui;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** 아직 만들지 않은 화면 자리. 실제 화면 클래스가 생기면 {@link MainFrame}에서 바꿔 끼우고 지운다. */
class PlaceholderScreen extends JPanel {

    PlaceholderScreen(String description) {
        super(new BorderLayout());
        add(new JLabel(description, SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
