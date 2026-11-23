package silverbell.client.ui.game;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import silverbell.client.net.MessageRouter;
import silverbell.common.MessageType;

/**
 * 점수판 영역: 플레이어별 점수를 높은 순으로.
 * UI 명세서 S4 게임 화면 오른쪽 아래 (F2 점수판).
 *
 * <p><b>담당: 라인 A</b>. 이 영역은 이 클래스만 고친다. 다른 영역을 고칠 일이 생기면 그 담당과 먼저 이야기한다.
 */
class ScorePanel extends JPanel {

    private final DefaultListModel<String> scoreListModel = new DefaultListModel<>();
    private final JList<String> scoreList = new JList<>(scoreListModel);

    ScorePanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("점수판 (라인 A)"));

        scoreList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        scoreList.setFocusable(false);
        add(new JScrollPane(scoreList), BorderLayout.CENTER);
    }

    /**
     * 이 영역이 받을 메시지를 등록한다. {@link GameScreen}이 접속 전에 한 번 부른다.
     *
     * <pre>
     * SCORE  사람별 점수를 높은 순으로 다시 그리기 (#12)
     * </pre>
     */
    void register(MessageRouter router) {
        router.on(MessageType.SCORE, msg -> {
            List<String> rows = parseScoreRows(msg.values(0));
            SwingUtilities.invokeLater(() -> updateScores(rows));
        });
    }

    private List<String> parseScoreRows(String[] tokens) {
        List<String> rows = new ArrayList<>();
        if (tokens == null) {
            return rows;
        }
        int rank = 1;
        for (String token : tokens) {
            int sep = token.lastIndexOf(':');
            if (sep <= 0 || sep == token.length() - 1) {
                continue;
            }
            String nickname = token.substring(0, sep);
            String score = token.substring(sep + 1);
            rows.add(rank + "위  " + nickname + " : " + score + "점");
            rank++;
        }
        return rows;
    }

    private void updateScores(List<String> rows) {
        scoreListModel.clear();
        for (String row : rows) {
            scoreListModel.addElement(row);
        }
    }
}