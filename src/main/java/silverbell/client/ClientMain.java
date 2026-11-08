package silverbell.client;

import javax.swing.SwingUtilities;

import silverbell.client.ui.MainFrame;

/**
 * 클라이언트 시작점. 게임 창을 띄운다.
 *
 * <p>Swing 화면은 반드시 이벤트 디스패치 스레드(EDT)에서 만들고 고쳐야 해서
 * {@link SwingUtilities#invokeLater}로 감싼다. 서버에서 메시지를 받는 스레드가 화면을 고칠 때도 같다.
 */
public final class ClientMain {

    private ClientMain() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
