package silverbell.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HintMakerTest {

    @Test
    @DisplayName("띄어쓰기 유지 및 1차·2차 초성 변환 (PRD C4 예시)")
    void make_withSpaces() {
        String answer = "피타고라스의 정리";

        assertEquals("ㅍ○○○○○ ○○", HintMaker.make(answer, 1));
        assertEquals("ㅍㅌㄱㄹㅅㅇ ㅈㄹ", HintMaker.make(answer, 2));
    }

    @Test
    @DisplayName("받침 있는 글자 포함 정답 초성 추출")
    void make_withBatchim() {
        String answer = "한성대학교";

        assertEquals("ㅎ○○○○", HintMaker.make(answer, 1));
        assertEquals("ㅎㅅㄷㅎㄱ", HintMaker.make(answer, 2));
    }

    @Test
    @DisplayName("영문·숫자 섞인 정답은 한글 외 문자를 ○로 마스킹")
    void make_withEnglishAndDigits() {
        String answer = "Java 17 버전";

        assertEquals("○○○○ ○○ ○○", HintMaker.make(answer, 1));
        assertEquals("○○○○ ○○ ㅂㅈ", HintMaker.make(answer, 2));
    }

    @Test
    @DisplayName("한 글자 정답은 1차와 2차 모두 해당 글자의 초성만 반환")
    void make_singleCharacter() {
        String answer = "별";

        assertEquals("ㅂ", HintMaker.make(answer, 1));
        assertEquals("ㅂ", HintMaker.make(answer, 2));
    }

    @Test
    @DisplayName("4지선다 문제는 힌트를 생성하지 않고 빈 문자열 반환")
    void make_multipleChoiceReturnsEmpty() {
        Question q = new Question(
                "4지선다", "2", "1+1은?",
                List.of("1", "2", "3", "4"),
                List.of("2")
        );

        assertEquals("", HintMaker.make(q, 1));
        assertEquals("", HintMaker.make(q, 2));
    }
}