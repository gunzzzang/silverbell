package silverbell.server.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import silverbell.common.GameConfig;
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

    private final Broadcaster broadcaster;
    private final QuestionDeck questionDeck;
    private final QuestionTimer questionTimer;
    /** 이번 문제에 벨을 울린 순서 (#14). 새 문제마다 비운다 */
    private final BellQueue bellQueue = new BellQueue();
    private final ScheduledExecutorService roundScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "round-scheduler");
        t.setDaemon(true);
        return t;
    });

    private final List<Player> players = new ArrayList<>();
    private boolean isGaming = false;
    private boolean roundActive = false;
    private int questionNumber = 0;
    private Question currentQuestion;
    private ScheduledFuture<?> nextQuestionTask;

    /** 기존 #5 테스트 호환용 생성자 */
    public GameRoom(Broadcaster broadcaster) {
        this(broadcaster, new QuestionDeck(), new QuestionTimer());
    }

    /** 서버 실행 및 라운드 테스트용 생성자 */
    public GameRoom(Broadcaster broadcaster, QuestionDeck questionDeck) {
        this(broadcaster, questionDeck, new QuestionTimer());
    }

    GameRoom(Broadcaster broadcaster, QuestionDeck questionDeck, QuestionTimer questionTimer) {
        this.broadcaster = broadcaster;
        this.questionDeck = questionDeck;
        this.questionTimer = questionTimer;
    }

    /**
     * 입장 가능 여부를 검사하고, 가능하면 참가자 목록에 추가한다. (동기화 필수)
     */
    @Override
    public synchronized String tryJoin(String rawNickname) {
        if (isGaming) {
            return JoinFailReason.GAME_IN_PROGRESS;
        }
        if (players.size() >= GameConfig.MAX_PLAYERS) {
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
        broadcaster.broadcast(Message.of(MessageType.SYSTEM, nickname + "님이 입장했습니다."));
    }

    /**
     * 연결이 끊긴 뒤 호출된다.
     */
    @Override
    public synchronized void left(String nickname) {
        boolean removed = players.removeIf(p -> p.getNickname().equals(nickname));
        if (removed) {
            broadcaster.broadcast(Message.of(MessageType.SYSTEM, nickname + "님이 퇴장했습니다."));
            if (!players.isEmpty()) {
                // 방장이 나갔다면 남은 첫 번째 유저에게 방장 위임
                if (players.stream().noneMatch(Player::isHost)) {
                    players.get(0).setHost(true);
                }
                broadcastUsersList();
            } else {
                // 모두 나가면 게임 상태를 초기화해 빈 방에서 문제가 계속 돌지 않게 함
                isGaming = false;
                roundActive = false;
                questionTimer.stop();
                if (nextQuestionTask != null) {
                    nextQuestionTask.cancel(false);
                }
            }
        }
    }

    /**
     * 방장이 START를 눌렀을 때 게임을 시작하고 첫 문제를 출제한다 (#11 임시 시작, #19에서 준비 검증 연결).
     * 방장이 아니거나 이미 게임 중이면 무시한다.
     */
    public synchronized void startGame(String requesterNickname) {
        if (isGaming) {
            return;
        }
        boolean isHost = players.stream()
                .anyMatch(p -> p.getNickname().equals(requesterNickname) && p.isHost());
        if (!isHost) {
            return;
        }
        if (questionDeck.totalLoadedCount() == 0) {
            broadcaster.broadcast(Message.of(MessageType.SYSTEM, "출제할 문제가 없습니다."));
            return;
        }

        this.isGaming = true;
        this.questionNumber = 0;
        startNextQuestion();
    }

    /**
     * 덱에서 문제를 꺼내 모든 참가자에게 QUESTION을 보내고 문제 타이머를 시작한다 (C1, C2).
     */
    public synchronized void startNextQuestion() {
        if (!isGaming) {
            return;
        }
        this.currentQuestion = questionDeck.drawNext();
        this.questionNumber++;
        this.roundActive = true;
        bellQueue.reset(); // 새 문제마다 벨 줄·틀린 사람·힌트 기록을 비운다 (#14)

        // 단답형은 choices가 비어 있으므로 4칸을 빈 문자열로 채워 프로토콜 9칸 형식을 유지
        List<String> choices = currentQuestion.getChoices();
        String c1 = choices.size() > 0 ? choices.get(0) : "";
        String c2 = choices.size() > 1 ? choices.get(1) : "";
        String c3 = choices.size() > 2 ? choices.get(2) : "";
        String c4 = choices.size() > 3 ? choices.get(3) : "";

        // MessageType.QUESTION 규약: 번호;유형;난이도;제한 시간;보기1;보기2;보기3;보기4;문제(마지막)
        Message questionMsg = Message.of(
                MessageType.QUESTION,
                String.valueOf(questionNumber),
                currentQuestion.getType(),
                currentQuestion.getDifficulty(),
                String.valueOf(GameConfig.QUESTION_TIME_MS),
                c1, c2, c3, c4,
                currentQuestion.getPrompt()
        );

        broadcaster.broadcast(questionMsg);
        questionTimer.start(GameConfig.QUESTION_TIME_MS, this::finishRound);
    }

    /**
     * 지금 문제가 떠 있는지(라운드 진행 중인지) 반환한다.
     * #14가 벨 입력을 받을지 판단할 때 호출한다.
     */
    public synchronized boolean isRoundActive() {
        return roundActive;
    }

    /**
     * 벨을 울린다 (#14, D1 벨 누르기·D2 벨 대기열·D3 순서 표시). {@code BUZZ} 핸들러가 부른다.
     *
     * <p>문제가 떠 있을 때만 받는다. 줄에 들어가면 모두에게 알림과 줄 순서({@code QUEUE})를 보내고,
     * 줄 맨 앞이 됐으면 지금 차례({@code TURN})도 보낸다. 벨을 울린 순간의 힌트 수도 같이 기록한다(F1).
     * 문제가 없을 때, 이미 줄에 있을 때, 이번 문제에서 틀렸을 때는 무시한다(N7).
     *
     * <pre>
     * 주성 벨 → SYSTEM|주성 님이 벨을 울렸어요!  QUEUE|주성       TURN|주성;10000
     * 이건 벨 → SYSTEM|이건 님이 벨을 울렸어요!  QUEUE|주성;이건  (차례는 그대로 주성)
     * </pre>
     *
     * <p>이 객체를 잠근 채 보낸다. 잠금 밖에서 보내면 그사이 {@link #finishRound()}가 끼어들어
     * {@code REVEAL} 뒤에 {@code QUEUE}·{@code TURN}이 도착할 수 있다.
     */
    public synchronized void buzz(String nickname) {
        if (!roundActive) {
            return;
        }
        if (!bellQueue.add(nickname, getRevealedHintCount())) {
            return;
        }
        broadcaster.broadcast(Message.of(MessageType.SYSTEM, nickname + " 님이 벨을 울렸어요!"));
        List<String> order = bellQueue.order();
        broadcaster.broadcast(Message.of(MessageType.QUEUE, order.toArray(new String[0])));
        if (order.size() == 1) { // 빈 줄에 처음 들어왔으면 이 사람이 지금 차례
            broadcaster.broadcast(Message.of(MessageType.TURN, nickname, String.valueOf(GameConfig.ANSWER_TIME_MS)));
        }
    }

    /**
     * 현재 라운드를 끝내고 정답을 공개(REVEAL)한 뒤 {@code GameConfig.NEXT_QUESTION_DELAY_MS}만큼 기다렸다 다음 문제로 넘어간다.
     * <p>문제 시간이 끝났을 때 타이머가 호출하며, 정답이 나왔을 때(#16)와 {@code /skip} 명령 시(#25)에도 호출한다.
     */
    public synchronized void finishRound() {
        if (!roundActive) {
            return; // 타이머 만료와 정답 입력이 동시에 발생해도 한 번만 실행되도록 방어
        }
        this.roundActive = false;
        questionTimer.stop();

        String revealAnswer = formatRevealAnswer(currentQuestion);
        broadcaster.broadcast(Message.of(MessageType.REVEAL, revealAnswer));

        if (nextQuestionTask != null && !nextQuestionTask.isCancelled()) {
            nextQuestionTask.cancel(false);
        }
        nextQuestionTask = roundScheduler.schedule(
                this::startNextQuestion,
                GameConfig.NEXT_QUESTION_DELAY_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private String formatRevealAnswer(Question q) {
        if ("4지선다".equals(q.getType())) {
            // answers.get(0)은 "1"~"4" 번호이므로 보기 텍스트도 함께 표시
            int idx = Integer.parseInt(q.getAnswers().get(0)) - 1;
            return (idx + 1) + "번 (" + q.getChoices().get(idx) + ")";
        }
        return String.join(" / ", q.getAnswers());
    }

    /**
     * 지금 공개된 힌트 수를 반환한다.
     * M2에서는 항상 0을 돌려주고 #18에서 채운다. #14가 벨을 울린 순간 기록한다.
     */
    public synchronized int getRevealedHintCount() {
        return 0;
    }

    /**
     * 문제 타이머를 일시정지한다.
     * <p>사용법: 라인 B가 답변 차례 동안 멈출 때(#21) 이 메서드만 호출한다.
     */
    public synchronized void pauseQuestionTimer() {
        if (roundActive) {
            questionTimer.pause();
        }
    }

    /**
     * 일시정지된 문제 타이머를 이어서 재개한다.
     * <p>사용법: 답변 차례가 끝나 아무도 답하는 사람이 없을 때(#21) 이 메서드만 호출한다.
     */
    public synchronized void resumeQuestionTimer() {
        if (roundActive) {
            questionTimer.resume();
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
        if (nickname.contains(" ") || nickname.contains(";") || nickname.contains("|") || nickname.contains(":")) {
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

    // 리스트와 내부 Player 객체 모두 값만 복사해 반환 (외부 상태 변조 및 동시성 오류 방지)
    public synchronized List<Player> getPlayers() {
        List<Player> snapshot = new ArrayList<>(players.size());
        for (Player p : players) {
            snapshot.add(new Player(p));
        }
        return snapshot;
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

        private Player(Player other) {
            this.nickname = other.nickname;
            this.isHost = other.isHost;
            this.isReady = other.isReady;
        }

        public String getNickname() { return nickname; }
        public boolean isHost() { return isHost; }
        void setHost(boolean host) { isHost = host; }
        public boolean isReady() { return isReady; }
        void setReady(boolean ready) { isReady = ready; }
    }
}