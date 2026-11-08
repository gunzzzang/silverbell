package silverbell.server.game.answer;

import java.util.List;

/**
 * 답안이 정답인지 판정하는 방법. 문제 유형마다 구현이 하나씩 있다. (PRD E2 서버 채점)
 * Strategy 패턴으로 OCP(유형 추가 때 기존 코드 그대로)와 LSP(구현을 바꿔 끼워도 같은 약속)를 지킨다.
 *
 * <ul>
 *   <li>{@link MultipleChoiceChecker} — 4지선다, 번호 비교</li>
 *   <li>{@link ShortAnswerChecker} — 단답형, 띄어쓰기·대소문자 무시하고 정답 후보 중 하나와 같으면 정답</li>
 * </ul>
 *
 * <p>채점하는 쪽은 유형을 따지지 않고 {@code checker.isCorrect(...)}만 부른다.
 * 새 유형(예: OX)이 생기면 구현 클래스만 추가한다.
 *
 * <p><b>모든 구현이 지킬 약속(LSP)</b>: 어떤 입력이 와도(빈 문자열, 공백, 엉뚱한 값) 예외를 던지지 않고
 * {@code true}/{@code false}만 돌려준다. 그래야 채점하는 쪽이 구현을 바꿔 끼워도 그대로 동작한다.
 */
public interface AnswerChecker {

    /**
     * @param answers 정답 후보들. 4지선다는 정답 번호 하나("4"), 단답형은 문제 파일의 {@code /}로 나눈 표기들
     * @param input   플레이어가 보낸 답안
     */
    boolean isCorrect(List<String> answers, String input);
}
