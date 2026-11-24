package silverbell.server.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import silverbell.common.AnswerResult;
import silverbell.common.GameConfig;
import silverbell.common.JoinFailReason;
import silverbell.common.Message;
import silverbell.common.MessageType;
import silverbell.server.game.answer.AnswerChecker;
import silverbell.server.game.answer.AnswerCheckerSelector;

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
 *   <li>M2 (12주차) · 문제 진행, 라운드 시작·끝, 문제 타이머 (#11) · 벨 받기 {@link #buzz(String)} (#14, 라인 B 작은 PR)
 *       · 답안 받기 {@link #answer(String, String)} (#16, 라인 B 작은 PR)</li>
 *   <li>M3 (13주차) · 준비 상태와 시작 조건(#19), 한 판 종료(#20), 퇴장·방장 넘기기(#22, 라인 B가 작은 PR로)
 *       · 답변 시간·다음 차례·재도전 금지 {@code passTurn} (#21, 라인 B 작은 PR)</li>
 * </ul>
 */
public class GameRoom implements ConnectionListener {

    private final Broadcaster broadcaster;
    private final QuestionDeck questionDeck;
    private final QuestionTimer questionTimer;
    /**
     * 답변 시간(D4)을 재는 타이머 (#21). 「정해진 시간이 지나면 한 번 부르고, 멈출 수 있는」 일이 문제 타이머와 같아서
     * 같은 클래스({@link QuestionTimer})를 하나 더 만들어 쓴다. 타이머 코드(#11)는 고치지 않는다.
     */
    private final QuestionTimer answerTimer;
    private final ScoreBoard scoreBoard = new ScoreBoard();
    /** 이번 문제에 벨을 울린 순서 (#14). 새 문제마다 비운다 */
    private final BellQueue bellQueue = new BellQueue();
    /** 문제 유형에 맞는 채점기를 골라 준다 (#15). 상태가 없어 하나만 둔다 */
    private final AnswerCheckerSelector checkerSelector = new AnswerCheckerSelector();
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
        this(broadcaster, new QuestionDeck(), new QuestionTimer(), new QuestionTimer());
    }

    /** 서버 실행 및 라운드 테스트용 생성자 */
    public GameRoom(Broadcaster broadcaster, QuestionDeck questionDeck) {
        this(broadcaster, questionDeck, new QuestionTimer(), new QuestionTimer());
    }

    /** 테스트용: 문제 타이머와 답변 타이머를 바꿔 넣어 시간을 기다리지 않고 확인한다 (#21). */
    GameRoom(Broadcaster broadcaster, QuestionDeck questionDeck, QuestionTimer questionTimer, QuestionTimer answerTimer) {
        this.broadcaster = broadcaster;
        this.questionDeck = questionDeck;
        this.questionTimer = questionTimer;
        this.answerTimer = answerTimer;
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
        scoreBoard.addPlayer(nickname);
        broadcastUsersList();
        broadcastScoreBoard();
        broadcaster.broadcast(Message.of(MessageType.SYSTEM, nickname + "님이 입장했습니다."));
    }

    /**
     * 연결이 끊긴 뒤 호출된다.
     */
    @Override
    public synchronized void left(String nickname) {
        boolean removed = players.removeIf(p -> p.getNickname().equals(nickname));
        if (removed) {
            scoreBoard.removePlayer(nickname);
            broadcaster.broadcast(Message.of(MessageType.SYSTEM, nickname + "님이 퇴장했습니다."));
            if (!players.isEmpty()) {
                // 방장이 나갔다면 남은 첫 번째 유저에게 방장 위임
                if (players.stream().noneMatch(Player::isHost)) {
                    players.get(0).setHost(true);
                }
                broadcastUsersList();
                broadcastScoreBoard();
            } else {
                // 모두 나가면 게임 상태를 초기화해 빈 방에서 문제가 계속 돌지 않게 함
                isGaming = false;
                roundActive = false;
                questionTimer.stop();
                answerTimer.stop(); // 답변 중에 모두 나가도 시간 초과가 오지 않게 (#21)
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
     * 줄 맨 앞이 됐으면 차례를 시작한다({@code startTurn}: {@code TURN}, 문제 시간 멈춤, 답변 시간 재기 — #21).
     * 벨을 울린 순간의 힌트 수도 같이 기록한다(F1).
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
            startTurn(nickname);
        }
    }

    /**
     * 답안을 받아 채점하고 모두에게 판정을 알린다 (#16, E1 답안 입력·E2 서버 채점·E3 판정 알림). {@code ANSWER} 핸들러가 부른다.
     *
     * <p>문제가 떠 있고 보낸 사람이 지금 차례(벨 줄 맨 앞)일 때만 채점한다. 아니면 무시한다(N7).
     * 화면이 차례가 아닐 때 답 입력을 꺼도, 직접 {@code ANSWER}를 보내는 경우를 여기서 한 번 더 막는다.
     *
     * <pre>
     * 정답 → SYSTEM|이건 님 정답! +2점   RESULT|이건;정답;2    SCORE|…   REVEAL|… (라운드 끝내기)
     * 오답 → {@code passTurn}: 감점하고 줄 다음 사람에게 차례를 넘긴다 (#21)
     * </pre>
     *
     * <p>정답 점수는 벨을 울린 순간의 힌트 수({@link BellQueue#hintCountOf(String)})로 {@link ScoreBoard}가 계산한다(F1).
     *
     * <p>{@link #buzz(String)}처럼 이 객체를 잠근 채 확인·채점·전송을 한다. 그래야 같은 순간 문제 시간이 끝나도
     * {@code REVEAL} 뒤에 {@code RESULT}가 가거나, 라운드가 두 번 끝나지 않는다.
     */
    public synchronized void answer(String nickname, String input) {
        if (!roundActive) {
            return;
        }
        if (!nickname.equals(bellQueue.current())) { // 차례가 아닌 사람의 답 (줄에 없거나 뒤에 있음)
            return;
        }
        AnswerChecker checker = checkerSelector.select(currentQuestion.getType());
        if (checker.isCorrect(currentQuestion.getAnswers(), input)) {
            int earned = scoreBoard.addCorrectScore(nickname, currentQuestion.getDifficulty(), bellQueue.hintCountOf(nickname));
            broadcaster.broadcast(Message.of(MessageType.SYSTEM, nickname + " 님 정답! +" + earned + "점"));
            broadcaster.broadcast(Message.of(MessageType.RESULT, nickname, AnswerResult.CORRECT, String.valueOf(earned)));
            broadcastScoreBoard();
            finishRound(); // #11의 라운드 끝내기: 정답 공개 후 다음 문제 예약 (답변 타이머도 여기서 멈춘다)
        } else {
            passTurn(nickname, AnswerResult.WRONG);
        }
    }

    /**
     * 줄 맨 앞이 된 사람의 답변 차례를 시작한다 (#21, D3 순서 표시·D4 답변 시간).
     * 모두에게 {@code TURN}을 보내고, 차례 동안 문제 시간을 멈추고(C2), 답변 시간을 처음부터 잰다.
     * 문제 시간이 얼마 남았든 답변 시간은 따로 재므로, 1초 남기고 울려도 답변 시간을 다 쓸 수 있다.
     *
     * <p>{@link #buzz(String)}(빈 줄에 첫 벨)와 {@code passTurn}(앞사람이 틀림)이 이 객체를 잠근 채 부른다.
     */
    private void startTurn(String nickname) {
        broadcaster.broadcast(Message.of(MessageType.TURN, nickname, String.valueOf(GameConfig.ANSWER_TIME_MS)));
        pauseQuestionTimer(); // #11 기능 호출: 답하는 동안 문제 시간·힌트 시간 멈춤
        int turnQuestion = questionNumber; // 타이머가 늦게 불렸을 때 어느 문제의 차례였는지 확인하려고 기억해 둔다
        answerTimer.start(GameConfig.ANSWER_TIME_MS, () -> answerTimeUp(nickname, turnQuestion)); // 시간이 다 되면 타이머 스레드가 부름
    }

    /**
     * 답변 시간이 끝났을 때 답변 타이머 스레드가 부른다 (#21, D4 답변 시간). 시간 초과는 오답처럼 처리한다.
     *
     * <p>타이머는 시간이 다 된 순간 잠금 밖에서 이 메서드를 부르므로, 그사이 그 사람이 답을 보냈거나 라운드가 끝났을 수 있다.
     * 그래서 「아직 같은 문제이고, 그 사람이 아직 차례인가」를 잠근 뒤 다시 확인하고 아니면 무시한다.
     * 확인하지 않으면 정답을 보낸 바로 그 순간 시간이 끝났을 때, 끝난 차례에 시간 초과 감점이 한 번 더 들어간다.
     */
    synchronized void answerTimeUp(String nickname, int turnQuestion) {
        if (!roundActive || turnQuestion != questionNumber) {
            return;
        }
        if (!nickname.equals(bellQueue.current())) { // 이미 답해서 차례가 넘어감
            return;
        }
        passTurn(nickname, AnswerResult.TIMEOUT);
    }

    /**
     * 지금 차례인 사람이 틀렸을 때(오답·시간 초과) 감점하고 줄 다음 사람에게 차례를 넘긴다 (#21, D5 다음 차례·D6 재도전 금지).
     *
     * <pre>
     * 다음 사람 있음 → SYSTEM|주성 님 오답! -1점 · 이건 님 차례예요   RESULT|주성;오답;-1   SCORE|…   QUEUE|이건   TURN|이건;10000
     * 줄이 빔       → SYSTEM|주성 님 시간 초과! -1점                 RESULT|주성;시간 초과;-1   SCORE|…   QUEUE|   TURN|  (문제 시간 다시 흐름)
     * </pre>
     *
     * <p>{@link BellQueue#next()}가 그 사람을 줄에서 빼면서 틀린 사람으로 기록하므로, 이번 문제에서 다시 벨을 울려도
     * {@link #buzz(String)}가 무시한다(D6, N7). 줄이 비면 아직 안 울린 사람이 남은 문제 시간 동안 벨을 울릴 수 있다(D5).
     * {@code RESULT}를 {@code QUEUE}보다 먼저 보낸다. 화면은 {@code RESULT}로 「다시 못 울려요」를 띄우고, 뒤에 오는 줄에 내가 없어도 그 안내를 남긴다.
     *
     * @param result {@link AnswerResult#WRONG} 또는 {@link AnswerResult#TIMEOUT}
     */
    private void passTurn(String nickname, String result) {
        answerTimer.stop(); // 오답이면 남은 답변 시간을 버린다 (시간 초과면 이미 멈춰 있음)
        int delta = scoreBoard.applyPenalty(nickname); // -1 (음수 허용, Q6)
        String next = bellQueue.next(); // 틀린 사람을 빼고 기록, 다음 차례를 받는다

        String notice = nickname + " 님 " + result + "! " + delta + "점";
        if (next != null) {
            notice += " · " + next + " 님 차례예요"; // UI 명세서 M8
        }
        broadcaster.broadcast(Message.of(MessageType.SYSTEM, notice));
        broadcaster.broadcast(Message.of(MessageType.RESULT, nickname, result, String.valueOf(delta)));
        broadcastScoreBoard();
        broadcaster.broadcast(Message.of(MessageType.QUEUE, bellQueue.order().toArray(new String[0])));

        if (next != null) {
            startTurn(next);
        } else {
            broadcaster.broadcast(Message.of(MessageType.TURN)); // 차례 없음: 화면의 문제 시간 바가 다시 흐른다
            resumeQuestionTimer(); // #11 기능 호출: 남은 문제 시간을 이어서 잰다
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
        answerTimer.stop(); // 정답·문제 시간 끝·문제 넘기기 모두 답변 차례도 끝낸다 (#21)

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

    /**
     * 현재 점수판을 높은 점수 순으로 모든 참가자에게 전송한다 (F1, F2).
     * #16(정답·오답 처리)이나 #22(퇴장 처리)에서 ScoreBoard를 갱신한 뒤 호출한다.
     */
    public synchronized void broadcastScoreBoard() {
        String[] tokens = scoreBoard.toScoreTokens();
        if (tokens.length == 0) {
            return;
        }
        broadcaster.broadcast(Message.of(MessageType.SCORE, tokens));
    }

    public ScoreBoard getScoreBoard() {
        return scoreBoard;
    }

    public synchronized Question getCurrentQuestion() {
        return currentQuestion;
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