package silverbell.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import silverbell.common.Message;
import silverbell.common.MessageType;

/**
 * 답안 받기 {@link GameRoom#answer(String, String)} 테스트 (#16).
 * 차례 검증(N7), 채점 결과에 따른 판정 알림(E3), 점수(F1), 정답이면 라운드 끝내기(#11 finishRound 호출)를 확인한다.
 */
class GameRoomAnswerTest {

    /** 보통(2점) 단답형 한 문제. 정답 후보 두 개 */
    private static final String SHORT_QUESTION = "단답형|2|IP 주소에서 네트워크 부분과 호스트 부분을 나누는 값은?||서브넷 마스크/서브넷마스크\n";
    /** 쉬움(1점) 4지선다 한 문제. 덱이 보기를 섞으므로 정답 번호는 문제를 꺼낸 뒤 확인한다 */
    private static final String CHOICE_QUESTION = "4지선다|1|TCP는 몇 계층 프로토콜인가?|물리;데이터링크;네트워크;전송|4\n";
    private static final String[] PLAYERS = {"방장", "주성", "이건"};

    private final FakeBroadcaster broadcaster = new FakeBroadcaster();

    @Test
    @DisplayName("차례인 사람이 정답을 보내면 정답 알림·판정·점수판·정답 공개를 순서대로 보내고 라운드가 끝난다")
    void correctAnswerFinishesRound() throws IOException {
        // given
        GameRoom room = startedRoom(SHORT_QUESTION);
        room.buzz("주성");
        broadcaster.clear();

        // when
        room.answer("주성", "서브넷마스크");

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "주성 님 정답! +2점"),
                Message.of(MessageType.RESULT, "주성", "정답", "2"),
                Message.of(MessageType.SCORE, "주성:2", "방장:0", "이건:0"),
                Message.of(MessageType.REVEAL, "서브넷 마스크 / 서브넷마스크")
        ), broadcaster.broadcasts());
        assertFalse(room.isRoundActive());
    }

    @Test
    @DisplayName("차례인 사람이 오답을 보내면 오답 알림·판정·점수판만 보내고 라운드는 이어진다")
    void wrongAnswerKeepsRound() throws IOException {
        // given
        GameRoom room = startedRoom(SHORT_QUESTION);
        room.buzz("주성");
        broadcaster.clear();

        // when
        room.answer("주성", "게이트웨이");

        // then
        assertEquals(List.of(
                Message.of(MessageType.SYSTEM, "주성 님 오답! -1점"),
                Message.of(MessageType.RESULT, "주성", "오답", "-1"),
                Message.of(MessageType.SCORE, "방장:0", "이건:0", "주성:-1")
        ), broadcaster.broadcasts());
        assertTrue(room.isRoundActive());
    }

    @Test
    @DisplayName("4지선다에서 차례인 사람이 정답 번호를 보내면 난이도 점수를 얻는다")
    void correctChoiceNumber() throws IOException {
        // given
        GameRoom room = startedRoom(CHOICE_QUESTION);
        String correctNumber = room.getCurrentQuestion().getAnswers().get(0); // 섞인 뒤의 정답 번호
        room.buzz("이건");
        broadcaster.clear();

        // when
        room.answer("이건", correctNumber);

        // then
        assertEquals(Message.of(MessageType.RESULT, "이건", "정답", "1"), broadcaster.broadcasts().get(1));
    }

    @Test
    @DisplayName("줄 두 번째 사람이 답을 보내면 무시된다")
    void ignoreAnswerFromSecondInLine() throws IOException {
        // given
        GameRoom room = startedRoom(SHORT_QUESTION);
        room.buzz("주성");
        room.buzz("이건");
        broadcaster.clear();

        // when
        room.answer("이건", "서브넷 마스크");

        // then
        assertEquals(List.of(), broadcaster.broadcasts());
        assertTrue(room.isRoundActive());
    }

    @Test
    @DisplayName("벨을 울리지 않은 사람이 답을 보내면 무시된다")
    void ignoreAnswerWithoutBuzz() throws IOException {
        // given
        GameRoom room = startedRoom(SHORT_QUESTION);
        room.buzz("주성");
        broadcaster.clear();

        // when
        room.answer("방장", "서브넷 마스크");

        // then
        assertEquals(List.of(), broadcaster.broadcasts());
        assertTrue(room.isRoundActive());
    }

    @Test
    @DisplayName("게임이 시작되기 전에 답을 보내면 무시된다")
    void ignoreAnswerBeforeGame() throws IOException {
        // given
        GameRoom room = joinedRoom(SHORT_QUESTION);
        broadcaster.clear();

        // when
        room.answer("주성", "서브넷 마스크");

        // then
        assertEquals(List.of(), broadcaster.broadcasts());
    }

    @Test
    @DisplayName("정답으로 라운드가 끝난 뒤 같은 사람이 다시 답을 보내면 무시된다")
    void ignoreAnswerAfterRoundFinished() throws IOException {
        // given
        GameRoom room = startedRoom(SHORT_QUESTION);
        room.buzz("주성");
        room.answer("주성", "서브넷 마스크");
        broadcaster.clear();

        // when
        room.answer("주성", "서브넷 마스크");

        // then
        assertEquals(List.of(), broadcaster.broadcasts());
    }

    /** 세 명이 입장해 점수판에 올라 있고, 문제 하나가 덱에 있는 방. */
    private GameRoom joinedRoom(String questions) throws IOException {
        QuestionDeck deck = new QuestionDeck();
        deck.loadQuestions(new ByteArrayInputStream(questions.getBytes(StandardCharsets.UTF_8)));
        GameRoom room = new GameRoom(broadcaster, deck);
        for (String nickname : PLAYERS) {
            room.tryJoin(nickname); // 첫 사람(방장)이 방장이 된다
            room.joined(nickname);  // 점수판에 0점으로 오른다 (#12)
        }
        return room;
    }

    /** {@link #joinedRoom}에서 방장이 게임을 시작해 첫 문제가 떠 있는 방. */
    private GameRoom startedRoom(String questions) throws IOException {
        GameRoom room = joinedRoom(questions);
        room.startGame("방장");
        return room;
    }
}
