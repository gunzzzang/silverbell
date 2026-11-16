package silverbell.server.game.answer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShortAnswerCheckerTest {

    private final AnswerChecker checker = new ShortAnswerChecker();

    @Test
    @DisplayName("정답 후보와 같으면 정답이다")
    void sameAsAnswer() {
        // given
        List<String> answers = List.of("피타고라스의 정리");

        // when, then
        assertTrue(checker.isCorrect(answers, "피타고라스의 정리"));
    }

    @Test
    @DisplayName("정답 후보 중 하나와만 같아도 정답이다")
    void anyOfAnswers() {
        // given
        List<String> answers = List.of("피타고라스의 정리", "피타고라스 정리", "피타고라스");

        // when, then
        assertTrue(checker.isCorrect(answers, "피타고라스의 정리"));
        assertTrue(checker.isCorrect(answers, "피타고라스 정리"));
        assertTrue(checker.isCorrect(answers, "피타고라스"));
    }

    @Test
    @DisplayName("다른 답이나 일부만 맞힌 답은 오답이다")
    void wrongAnswer() {
        // given
        List<String> answers = List.of("서브넷 마스크", "서브넷마스크");

        // when, then
        assertFalse(checker.isCorrect(answers, "서브넷"));
        assertFalse(checker.isCorrect(answers, "게이트웨이"));
        assertFalse(checker.isCorrect(answers, "서브넷 마스크다"));
    }

    @Test
    @DisplayName("띄어쓰기는 앞뒤·중간 모두 무시한다")
    void ignoreSpaces() {
        // given
        List<String> answers = List.of("서브넷 마스크");

        // when, then
        assertTrue(checker.isCorrect(answers, "서브넷마스크"));
        assertTrue(checker.isCorrect(answers, " 서브넷  마스크 "));
        assertTrue(checker.isCorrect(answers, "서 브 넷 마 스 크"));
    }

    @Test
    @DisplayName("영어 대소문자는 무시한다")
    void ignoreCase() {
        // given
        List<String> answers = List.of("TCP", "Transmission Control Protocol");

        // when, then
        assertTrue(checker.isCorrect(answers, "tcp"));
        assertTrue(checker.isCorrect(answers, "Tcp"));
        assertTrue(checker.isCorrect(answers, "transmission control protocol"));
        assertTrue(checker.isCorrect(answers, "TRANSMISSIONCONTROLPROTOCOL"));
    }

    @Test
    @DisplayName("빈 문자열·공백만 보내면 예외 없이 오답이다")
    void emptyInput() {
        // given
        List<String> answers = List.of("서브넷 마스크");

        // when, then
        assertFalse(checker.isCorrect(answers, ""));
        assertFalse(checker.isCorrect(answers, "   "));
    }

    @Test
    @DisplayName("공백만 보낸 답은 정답 후보에 빈 값이 섞여 있어도 오답이다")
    void emptyInputNeverMatchesEmptyAnswer() {
        // given
        List<String> answers = List.of("", " ");

        // when, then
        assertFalse(checker.isCorrect(answers, ""));
        assertFalse(checker.isCorrect(answers, "  "));
    }

    @Test
    @DisplayName("답이 null이거나 정답 목록이 null·비어 있어도 예외 없이 오답이다")
    void nullInput() {
        // when, then
        assertFalse(checker.isCorrect(List.of("서브넷 마스크"), null));
        assertFalse(checker.isCorrect(null, "서브넷 마스크"));
        assertFalse(checker.isCorrect(new ArrayList<>(), "서브넷 마스크"));
    }

    @Test
    @DisplayName("정답 후보 중간에 null이 있어도 건너뛰고 나머지와 비교한다")
    void nullAmongAnswers() {
        // given
        List<String> answers = Arrays.asList(null, "서브넷 마스크"); // List.of는 null을 못 넣어서 Arrays.asList

        // when, then
        assertTrue(checker.isCorrect(answers, "서브넷마스크"));
        assertFalse(checker.isCorrect(answers, "게이트웨이"));
    }
}
