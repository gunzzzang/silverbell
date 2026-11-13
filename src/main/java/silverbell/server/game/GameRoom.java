package silverbell.server.game;

import java.util.ArrayList;
import java.util.List;
import silverbell.common.JoinFailReason;
import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 방 하나의 게임 상태. 참가자·방장·준비 상태, 지금 문제, 라운드 진행 여부, 점수를 가진다.
 * 여러 {@code ClientHandler} 스레드가 같이 쓰므로 공개 메서드는 이 클래스 안에서 동기화한다.
 *
 * <p>소켓을 모른다. 메시지를 보낼 때는 생성자로 받은 {@code Broadcaster}만 쓴다.
 *
 * <p>라인 A가 맡는다. 라인 B가 고칠 일이 생기면 그 변경만 작은 PR로 먼저 올린다.
 *
 * <p>할 일:
 * <ul>
 *   <li>M1 (11주차) · 입장, 닉네임 중복·인원 초과·게임 중 입장 거부, 방장 지정 (#5)</li>
 *   <li>M2 (12주차) · 문제 진행, 라운드 시작·끝, 문제 타이머 (#11)</li>
 *   <li>M3 (13주차) · 준비 상태와 시작 조건(#19), 한 판 종료(#20), 퇴장·방장 넘기기(#22, 라인 B가 작은 PR로)</li>
 * </ul>
 */
public class GameRoom implements ConnectionListener {
    private static final int MAX_PLAYERS = 4;

    private final Broadcaster broadcaster;
    private final List<Player> players = new ArrayList<>();
    private boolean isGaming = false;

    public GameRoom(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * 입장 가능 여부를 검사하고, 가능하면 참가자 목록에 추가한다. (동기화 필수)
     */
    @Override
    public synchronized String tryJoin(String rawNickname) {
        if (isGaming) {
            return JoinFailReason.GAME_IN_PROGRESS;
        }
        if (players.size() >= MAX_PLAYERS) {
            return JoinFailReason.ROOM_FULL;
        }

        String validationReason = validateNickname(rawNickname);
        if (validationReason != null) {
            return validationReason;
        }

        String nickname = rawNickname.strip();
        boolean isHost = players.isEmpty(); // 첫 접속자가 방장
        players.add(new Player(nickname, isHost));

        return null; // 입장 승인
    }

    /**
     * JOIN_OK 전송 및 수신 활성화 직후 호출된다.
     */
    @Override
    public synchronized void joined(String nickname) {
        broadcastUsersList();
    }

    /**
     * 연결이 끊긴 뒤 호출된다.
     */
    @Override
    public synchronized void left(String nickname) {
        boolean removed = players.removeIf(p -> p.getNickname().equals(nickname));
        if (removed && !players.isEmpty()) {
            // 방장이 나갔다면 남은 첫 번째 유저에게 방장 위임
            if (players.stream().noneMatch(Player::isHost)) {
                players.get(0).setHost(true);
            }
            broadcastUsersList();
        }
    }

    private String validateNickname(String rawNickname) {
        if (rawNickname == null) {
            return JoinFailReason.INVALID_NICKNAME_LENGTH;
        }
        String nickname = rawNickname.strip();
        if (nickname.isEmpty() || nickname.length() > 10) {
            return JoinFailReason.INVALID_NICKNAME_LENGTH;
        }
        if (nickname.contains(" ") || nickname.contains(";") || nickname.contains("|")) {
            return JoinFailReason.INVALID_NICKNAME_CHAR;
        }
        // net(ClientRegistry)에서 먼저 거르지만, GameRoom 단독 단위 테스트를 위해 방어 코드 유지
        for (Player p : players) {
            if (p.getNickname().equals(nickname)) {
                return JoinFailReason.DUPLICATE_NICKNAME;
            }
        }
        return null;
    }

    /**
     * USERS 브로드캐스트 (형식: USERS|닉네임1:HOST:NOT_READY;닉네임2:GUEST:NOT_READY)
     */
    private void broadcastUsersList() {
        if (players.isEmpty()) {
            return;
        }

        String[] userTokens = new String[players.size()];
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            userTokens[i] = p.getNickname() + ":"
                    + (p.isHost() ? "HOST" : "GUEST") + ":"
                    + (p.isReady() ? "READY" : "NOT_READY");
        }

        Message usersMessage = Message.of(MessageType.USERS, userTokens);
        broadcaster.broadcast(usersMessage);
    }

    // 방어적 복사본 반환 (외부 순회 시 ConcurrentModificationException 방지)
    public synchronized List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    // 테스트 및 추후 게임 진행 상태 제어용
    public synchronized void setGaming(boolean gaming) {
        this.isGaming = gaming;
    }

    public static class Player {
        private final String nickname;
        private boolean isHost;
        private boolean isReady;

        public Player(String nickname, boolean isHost) {
            this.nickname = nickname;
            this.isHost = isHost;
            this.isReady = false;
        }

        public String getNickname() { return nickname; }
        public boolean isHost() { return isHost; }
        public void setHost(boolean host) { isHost = host; }
        public boolean isReady() { return isReady; }
        public void setReady(boolean ready) { isReady = ready; }
    }
}
