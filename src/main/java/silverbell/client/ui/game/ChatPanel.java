package silverbell.client.ui.game;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import silverbell.client.net.MessageRouter;

/**
 * 게임 화면 채팅 영역: 채팅창({@code JScrollPane} + {@code JTextArea})과 입력칸.
 * UI 명세서 S4~S9 게임 화면 아래 (B1 채팅, B2 시스템 메시지, U1 배치).
 *
 * <p>게임 화면에서는 입력칸이 평소 닫혀 있다. Enter로 열고, 보내면 닫힌다(PRD B1).
 * 열려 있는 동안엔 입력칸이 포커스를 가져서 스페이스바·숫자키가 글자로 들어간다.
 * 닫을 때 {@link GameScreen#takeKeyFocus()}를 불러 키 입력을 게임 화면에 돌려준다.
 *
 * <p><b>담당: 라인 A</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 * 단, 입력칸 열기·닫기({@link #openInput()})는 #14(라인 B) 범위라 그 부분만 라인 A와 맞춰 고친다.
 *
 * <p>지금은 자리만 잡은 빈 패널이다(#7). 꾸미기는 M4(#26).
 */
class ChatPanel extends JPanel {

    ChatPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("채팅 (라인 A)"));
        add(new JLabel("채팅 · Enter로 입력칸 열기", SwingConstants.CENTER), BorderLayout.CENTER);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * CHAT     채팅창에 추가 (#6, 대기실 채팅과 같은 방식)
     * SYSTEM   [시스템] 머리말로 추가 (#6)
     * WHISPER  귓속말 추가 (#25)
     * </pre>
     */
    void register(MessageRouter router) {
        // 각 이슈에서 위 표의 종류를 router.on(...)으로 등록한다
    }

    /** 입력칸이 닫혀 있을 때 Enter를 누르면 {@link GameScreen}이 부른다. 입력칸을 열고 커서를 옮긴다. */
    void openInput() {
        // #14: 입력칸 보이기 + requestFocus(). 보내면 입력칸을 닫고 GameScreen.takeKeyFocus()로 키 입력 돌려주기
    }
}
