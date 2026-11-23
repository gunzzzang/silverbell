package silverbell.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.common.GameConfig;
import silverbell.common.Message;
import silverbell.common.MessageType;

class GameRoomBuzzTest {

    private static final String QUESTIONS = """
            4지선다|1|TCP는 몇 계층 프로토콜인가?|물리;데이터링크;네트워크;전송|4
            단답형|2|IP 주소에서 네트워크 부분과 호스트 부분을 나누는 값은?||서브넷 마스크/서브넷마스크
            """;
    private static final String[] PLAYERS = {"방장", "주성", "이건", "민수"};
    /** 동시 벨 테스트 반복 횟수 */
    private static final int REPEAT = 50;

    private RecordingBroadcaster broadcaster;
    private GameRoom room;

    @BeforeEach
    void setUp() throws IOException {
        broadcaster = new RecordingBroadcaster();
        room = newRoomWithPlayers(broadcaster);
    }

    @Test
    @DisplayName("문제가 나오기 전에 울린 벨은 무시된다")
    void ignoreBuzzBeforeQuestion() {
        // when
        room.buzz("주성");

        // then
        assertEquals(List.of(), broadcaster.sent());
    }

    @Test
    @DisplayName("문제 중에 처음 벨을 울리면 벨 알림·줄 순서·지금 차례를 보낸다")
    void firstBuzzSendsQueueAndTurn() {
        // given
        startQuestion();

        // when
        room.buzz("주성");

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "주성 님이 벨을 울렸어요!"),
                Message.of(MessageType.QUEUE, "주성"),
                Message.of(MessageType.TURN, "주성", String.valueOf(GameConfig.ANSWER_TIME_MS))
        ), broadcaster.sent());
    }

    @Test
    @DisplayName("두 번째 사람이 벨을 울리면 벨 알림과 줄 순서만 보내고 차례는 그대로다")
    void secondBuzzSendsQueueOnly() {
        // given
        startQuestion();
        room.buzz("주성");
        broadcaster.clear();

        // when
        room.buzz("이건");

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "이건 님이 벨을 울렸어요!"),
                Message.of(MessageType.QUEUE, "주성", "이건")
        ), broadcaster.sent());
    }

    @Test
    @DisplayName("같은 사람이 벨을 연타하면 두 번째부터는 무시된다")
    void ignoreRepeatedBuzz() {
        // given
        startQuestion();
        room.buzz("주성");
        broadcaster.clear();

        // when
        room.buzz("주성");

        // then
        assertEquals(List.of(), broadcaster.sent());
    }

    @Test
    @DisplayName("정답이 공개된 뒤 울린 벨은 무시된다")
    void ignoreBuzzAfterReveal() {
        // given
        startQuestion();
        room.finishRound();
        broadcaster.clear();

        // when
        room.buzz("주성");

        // then
        assertEquals(List.of(), broadcaster.sent());
    }

    @Test
    @DisplayName("다음 문제가 시작되면 줄이 비어 지난 문제에 울린 사람도 다시 첫 차례가 될 수 있다")
    void nextQuestionClearsQueue() {
        // given
        startQuestion();
        room.buzz("주성");
        room.buzz("이건");
        room.finishRound();
        room.startNextQuestion();
        broadcaster.clear();

        // when
        room.buzz("이건");

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "이건 님이 벨을 울렸어요!"),
                Message.of(MessageType.QUEUE, "이건"),
                Message.of(MessageType.TURN, "이건", String.valueOf(GameConfig.ANSWER_TIME_MS))
        ), broadcaster.sent());
    }

    @Test
    @DisplayName("4명이 동시에 벨을 울려도 모두 한 번씩 줄을 서고, 차례는 줄 맨 앞 한 명에게만 간다")
    void simultaneousBuzz() throws IOException, InterruptedException {
        for (int round = 0; round < REPEAT; round++) {
            // given
            RecordingBroadcaster recorder = new RecordingBroadcaster();
            GameRoom sameRoom = newRoomWithPlayers(recorder);
            sameRoom.startGame("방장");
            recorder.clear();
            Buzzer[] buzzers = new Buzzer[PLAYERS.length];
            for (int i = 0; i < PLAYERS.length; i++) {
                buzzers[i] = new Buzzer(sameRoom, PLAYERS[i]);
            }

            // when
            for (Buzzer buzzer : buzzers) {
                buzzer.start();
            }
            for (Buzzer buzzer : buzzers) {
                buzzer.join();
            }

            // then
            List<Message> queues = recorder.ofType(MessageType.QUEUE);
            List<Message> turns = recorder.ofType(MessageType.TURN);
            String[] finalOrder = queues.get(queues.size() - 1).values(0);
            assertEquals(PLAYERS.length, queues.size());
            assertEquals(new HashSet<>(Arrays.asList(PLAYERS)), new HashSet<>(Arrays.asList(finalOrder)));
            assertEquals(1, turns.size());
            assertEquals(finalOrder[0], turns.get(0).values(2)[0]); // 차례는 최종 줄의 맨 앞 사람
            assertEquals(1, queues.get(0).values(0).length); // 첫 QUEUE에는 한 명만
        }
    }

    private void startQuestion() {
        room.startGame("방장");
        broadcaster.clear();
    }

    private static GameRoom newRoomWithPlayers(RecordingBroadcaster recorder) throws IOException {
        QuestionDeck deck = new QuestionDeck();
        deck.loadQuestions(new ByteArrayInputStream(QUESTIONS.getBytes(StandardCharsets.UTF_8)));
        GameRoom newRoom = new GameRoom(recorder, deck);
        for (String nickname : PLAYERS) {
            newRoom.tryJoin(nickname); // 첫 사람(방장)이 방장이 된다
        }
        recorder.clear();
        return newRoom;
    }

    /** 동시 벨 테스트에서 한 사람 몫. 수업 예제처럼 extends Thread로 만든다. */
    private static class Buzzer extends Thread {
        private final GameRoom room;
        private final String nickname;

        Buzzer(GameRoom room, String nickname) {
            this.room = room;
            this.nickname = nickname;
        }

        @Override
        public void run() {
            room.buzz(nickname); // 여러 스레드가 같은 GameRoom에 동시에 벨
        }
    }

    /** 소켓 대신 보낸 메시지를 순서대로 모아 두는 Broadcaster. 여러 스레드가 부를 수 있어 동기화한다 */
    private static class RecordingBroadcaster implements Broadcaster {
        private final List<Message> sent = new ArrayList<>();

        @Override
        public synchronized void broadcast(Message message) {
            sent.add(message);
        }

        @Override
        public synchronized void sendTo(String nickname, Message message) {
            sent.add(message);
        }

        synchronized List<Message> sent() {
            return new ArrayList<>(sent);
        }

        synchronized List<Message> ofType(MessageType type) {
            List<Message> found = new ArrayList<>();
            for (Message message : sent) {
                if (message.type() == type) {
                    found.add(message);
                }
            }
            return found;
        }

        synchronized void clear() {
            sent.clear();
        }
    }
}
