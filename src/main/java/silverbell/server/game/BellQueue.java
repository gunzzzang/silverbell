package silverbell.server.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 한 라운드에서 벨을 울린 사람을 서버 도착 순서대로 세운 줄. (PRD D2 벨 대기열)
 *
 * <p>줄 맨 앞 사람이 지금 답변 차례다(D3). 틀리면 그 사람을 줄에서 빼고 틀린 사람으로 기록해,
 * 다음 사람에게 차례가 넘어간다(D5). 틀린 사람은 이번 라운드에 다시 줄을 설 수 없다(D6 재도전 금지).
 * 라운드가 끝나면(정답·시간 끝·문제 넘기기) {@link #reset()}으로 비운다.
 *
 * <pre>
 * add("주성") → true   줄: [주성]        지금 차례: 주성
 * add("이건") → true   줄: [주성, 이건]
 * add("주성") → false  이미 줄에 있음 (연타)
 * next()      → "이건"  주성은 틀린 사람으로 기록, 줄: [이건]
 * add("주성") → false  이번 라운드에 틀림
 * reset()              줄·틀린 사람 모두 비움, 다음 문제에서 다시 울릴 수 있음
 * </pre>
 *
 * <p>사람은 닉네임으로 구분한다. 소켓을 모르고, 메시지도 보내지 않는다(#14에서 {@code BUZZ} 핸들러가 이 클래스를 쓴다).
 *
 * <p>여러 {@code ClientHandler} 스레드가 동시에 부르므로 공개 메서드를 모두 {@code synchronized}로 한다(N1 벨 순서 공정성).
 * 필드는 밖에 내주지 않고, 줄은 {@link #order()}로 복사본만 준다.
 */
public class BellQueue {

    /** 벨을 울린 순서. 0번이 지금 답변 차례 */
    private final List<String> line = new ArrayList<>();
    /** 이번 라운드에 틀린 사람 */
    private final List<String> wrong = new ArrayList<>();

    /**
     * 벨을 울린 사람을 줄 맨 뒤에 넣는다. 이미 줄에 있거나 이번 라운드에 틀린 사람이면 무시한다.
     *
     * @return 줄에 들어갔으면 {@code true}, 무시했으면 {@code false}
     */
    public synchronized boolean add(String nickname) { // 확인과 넣기를 한 스레드씩: 동시에 눌러도 섞이거나 두 번 들어가지 않음
        Objects.requireNonNull(nickname, "nickname");
        if (line.contains(nickname) || wrong.contains(nickname)) {
            return false;
        }
        line.add(nickname);
        return true;
    }

    /** 지금 답변 차례인 사람(줄 맨 앞). 줄이 비어 있으면 {@code null}. */
    public synchronized String current() {
        if (line.isEmpty()) {
            return null;
        }
        return line.get(0);
    }

    /**
     * 지금 차례인 사람이 틀렸을 때(오답·시간 초과) 부른다. 그 사람을 줄에서 빼서 틀린 사람으로 기록하고,
     * 다음 사람에게 차례를 넘긴다. 줄이 비어 있으면 아무것도 하지 않는다.
     *
     * @return 새로 차례가 된 사람. 줄이 비었으면 {@code null}(아직 안 울린 사람이 울릴 수 있다, D5)
     */
    public synchronized String next() { // 빼기와 틀림 기록을 한 번에: 그 사이에 같은 사람이 다시 줄 서지 못함
        if (line.isEmpty()) {
            return null;
        }
        String wrongPlayer = line.remove(0); // 맨 앞(지금 차례)을 빼면 뒷사람들이 한 칸씩 당겨짐
        wrong.add(wrongPlayer);
        return current();
    }

    /** 라운드를 새로 시작할 때 줄과 틀린 사람 기록을 모두 비운다. */
    public synchronized void reset() {
        line.clear();
        wrong.clear();
    }

    /** 줄에 선 사람 수. */
    public synchronized int size() {
        return line.size();
    }

    /** 줄 순서의 복사본. 0번이 지금 차례다. 받은 쪽이 고쳐도 줄은 바뀌지 않는다. */
    public synchronized List<String> order() {
        return List.copyOf(line);
    }
}
