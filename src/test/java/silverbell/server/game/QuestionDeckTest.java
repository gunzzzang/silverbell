package silverbell.server.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class QuestionDeckTest {

    @Test
    @DisplayName("샘플 형식 데이터 파싱 및 불량/빈 줄 건너뛰기 검증")
    void testLoadQuestionsWithErrors() throws IOException {
        String mockData =
                "4지선다|1|TCP는 몇 계층 프로토콜인가?|물리;데이터링크;네트워크;전송|4\n" +
                        "\n" + // 빈 줄
                        "잘못된_형식의_줄\n" + // 형식이 틀린 줄
                        "단답형|2|IP 주소에서 네트워크 부분과 호스트 부분을 나누는 값은?||서브넷 마스크/서브넷마스크\n" +
                        "단답형|3|직각삼각형 세 변의 관계를 나타낸 정리는?||피타고라스의 정리/피타고라스 정리\n";

        QuestionDeck deck = new QuestionDeck();
        deck.loadQuestions(new ByteArrayInputStream(mockData.getBytes(StandardCharsets.UTF_8)));

        // 잘못된 줄과 빈 줄은 건너뛰고 정상적인 3개 문제만 파싱되어야 함
        assertEquals(3, deck.totalLoadedCount());
    }

    @Test
    @DisplayName("이슈 기준 세부 불량 데이터(난이도, 유형, 보기 수, 정답 형식, 파이프 초과) 필터링 검증")
    void testInvalidFormatLinesAreSkipped() throws IOException {
        String invalidData =
                "4지선다|9|문제|a;b|7\n" +                 // 난이도 범위 오류(9), 보기 부족(2개), 정답 범위 오류(7)
                        "OX|1|문제||O\n" +                          // 지원하지 않는 유형(OX)
                        "4지선다|1|문제|a;b;c;d|전송\n" +             // 4지선다 정답이 숫자가 아님
                        "단답형|1|문제에 | 들어감||답|추가\n" +       // 칸 수 5개 초과(파이프 포함)
                        "4지선다|abc|문제|a;b;c;d|1\n" +             // 난이도가 숫자가 아님
                        "4지선다|1|문제|a;b;;d|1\n" +                // 4지선다 빈 보기 포함
                        "단답형|1|문제||답1/\n";                    // 단답형 빈 정답 후보 포함

        QuestionDeck deck = new QuestionDeck();
        deck.loadQuestions(new ByteArrayInputStream(invalidData.getBytes(StandardCharsets.UTF_8)));

        assertEquals(0, deck.totalLoadedCount());
    }

    @Test
    @DisplayName("단답형 정답 후보가 '/' 기준으로 분리되어 리스트로 저장되는지 검증")
    void testShortAnswerSplitIntoList() throws IOException {
        String mockData = "단답형|2|문제||서브넷 마스크/서브넷마스크\n";

        QuestionDeck deck = new QuestionDeck();
        deck.loadQuestions(new ByteArrayInputStream(mockData.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, deck.totalLoadedCount());
        Question q = deck.drawNext();
        assertEquals(Arrays.asList("서브넷 마스크", "서브넷마스크"), q.getAnswers());
    }

    @Test
    @DisplayName("한 바퀴를 도는 동안 중복 없이 문제를 모두 꺼내는지 검증")
    void testNoDuplicateInOneRound() throws IOException {
        String mockData =
                "4지선다|1|TCP는 몇 계층 프로토콜인가?|물리;데이터링크;네트워크;전송|4\n" +
                        "단답형|2|IP 주소에서 네트워크 부분과 호스트 부분을 나누는 값은?||서브넷 마스크/서브넷마스크\n" +
                        "단답형|3|직각삼각형 세 변의 관계를 나타낸 정리는?||피타고라스의 정리/피타고라스 정리\n";

        QuestionDeck deck = new QuestionDeck();
        deck.loadQuestions(new ByteArrayInputStream(mockData.getBytes(StandardCharsets.UTF_8)));

        Set<String> drawnPrompts = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            Question q = deck.drawNext();
            drawnPrompts.add(q.getPrompt());
        }

        // 3번 꺼냈을 때 3개의 문제가 모두 서로 달라야 함
        assertEquals(3, drawnPrompts.size());
    }

    @Test
    @DisplayName("4지선다 보기를 섞어도 원래 정답 보기의 내용이 유지되는지 검증")
    void testShuffleChoicesPreservesCorrectAnswerText() {
        List<String> choices = Arrays.asList("물리", "데이터링크", "네트워크", "전송");
        Question q = new Question("4지선다", "1", "TCP는 몇 계층 프로토콜인가?", choices, Collections.singletonList("4"));

        String originalAnswerText = choices.get(3); // "전송"

        // 보기 섞기
        q.shuffleChoices();

        int newAnswerIndex = Integer.parseInt(q.getAnswers().get(0)) - 1;
        String newAnswerText = q.getChoices().get(newAnswerIndex);

        // 보기 순서와 번호가 바뀌어도 갱신된 정답 번호의 보기는 여전히 "전송"이어야 함
        assertEquals(originalAnswerText, newAnswerText);
    }
}