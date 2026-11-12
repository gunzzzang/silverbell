package silverbell.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BellQueueTest {

    /** 동시 누르기 테스트: 10명이 5번씩 연타, 100회 반복 */
    private static final int PLAYERS = 10;
    private static final int PRESSES_PER_PLAYER = 5;
    private static final int REPEAT = 100;

    @Test
    @DisplayName("벨을 울린 순서대로 줄을 서고, 맨 앞 사람이 지금 차례다")
    void keepArrivalOrder() {
        // given
        BellQueue queue = new BellQueue();

        // when
        queue.add("주성");
        queue.add("이건");
        queue.add("민수");

        // then
        assertEquals(List.of("주성", "이건", "민수"), queue.order());
        assertEquals("주성", queue.current());
    }

    @Test
    @DisplayName("줄이 비어 있으면 지금 차례는 없다")
    void noTurnWhenEmpty() {
        // given
        BellQueue queue = new BellQueue();

        // when, then
        assertNull(queue.current());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("같은 사람이 벨을 두 번 누르면 한 번만 줄에 들어간다")
    void buzzTwice() {
        // given
        BellQueue queue = new BellQueue();

        // when
        boolean first = queue.add("이건");
        boolean second = queue.add("이건");

        // then
        assertTrue(first);
        assertFalse(second);
        assertEquals(1, queue.size());
    }

    @Test
    @DisplayName("다음 차례로 넘기면 맨 앞 사람이 빠지고 그다음 사람이 차례가 된다")
    void passToNext() {
        // given
        BellQueue queue = new BellQueue();
        queue.add("주성");
        queue.add("이건");

        // when
        String next = queue.next();

        // then
        assertEquals("이건", next);
        assertEquals("이건", queue.current());
        assertEquals(List.of("이건"), queue.order());
    }

    @Test
    @DisplayName("마지막 사람까지 넘기면 줄이 비고 지금 차례는 없다")
    void passLast() {
        // given
        BellQueue queue = new BellQueue();
        queue.add("주성");

        // when
        String next = queue.next();

        // then
        assertNull(next);
        assertNull(queue.current());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("줄이 비어 있을 때 다음 차례로 넘겨도 아무 일도 없다")
    void passWhenEmpty() {
        // given
        BellQueue queue = new BellQueue();

        // when
        String next = queue.next();

        // then
        assertNull(next);
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("틀려서 넘겨진 사람은 이번 라운드에 다시 벨을 울려도 무시된다")
    void wrongPlayerCannotBuzzAgain() {
        // given
        BellQueue queue = new BellQueue();
        queue.add("주성");
        queue.add("이건");
        queue.next(); // 주성 오답

        // when
        boolean added = queue.add("주성");

        // then
        assertFalse(added);
        assertEquals(List.of("이건"), queue.order());
    }

    @Test
    @DisplayName("줄이 다 빠진 뒤에도 아직 안 울린 사람은 벨을 울릴 수 있다")
    void othersCanBuzzAfterLineEmpties() {
        // given
        BellQueue queue = new BellQueue();
        queue.add("주성");
        queue.next(); // 주성 오답, 줄이 빔

        // when
        boolean added = queue.add("민수");

        // then
        assertTrue(added);
        assertEquals("민수", queue.current());
    }

    @Test
    @DisplayName("라운드를 초기화하면 줄이 비고 틀렸던 사람도 다시 벨을 울릴 수 있다")
    void resetRound() {
        // given
        BellQueue queue = new BellQueue();
        queue.add("주성");
        queue.add("이건");
        queue.next(); // 주성 오답

        // when
        queue.reset();

        // then
        assertEquals(0, queue.size());
        assertNull(queue.current());
        assertTrue(queue.add("주성"));
    }

    @Test
    @DisplayName("받은 줄 복사본을 고쳐도 원래 줄은 바뀌지 않는다")
    void orderIsCopy() {
        // given
        BellQueue queue = new BellQueue();
        queue.add("주성");
        List<String> order = queue.order();

        // when, then
        assertThrows(UnsupportedOperationException.class, () -> order.add("이건"));
        assertEquals(List.of("주성"), queue.order());
    }

    /** 동시 누르기 테스트에서 한 사람 몫. 벨을 여러 번 연타한다. */
    private static class Presser extends Thread {
        private final BellQueue queue;
        private final String nickname;

        Presser(BellQueue queue, String nickname) {
            this.queue = queue;
            this.nickname = nickname;
        }

        @Override
        public void run() {
            for (int i = 0; i < PRESSES_PER_PLAYER; i++) {
                queue.add(nickname); // 같은 BellQueue에 여러 스레드가 동시에 넣기
            }
        }
    }

    @Test
    @DisplayName("스레드 10개가 동시에 연타해도 매번 줄 길이 10, 중복 0이다 (100회 반복)")
    void concurrentBuzz() throws InterruptedException {
        for (int round = 1; round <= REPEAT; round++) {
            // given
            BellQueue queue = new BellQueue();
            Presser[] pressers = new Presser[PLAYERS];
            for (int i = 0; i < PLAYERS; i++) {
                pressers[i] = new Presser(queue, "플레이어" + i);
            }

            // when
            for (Presser presser : pressers) {
                presser.start(); // 10명이 거의 동시에 벨 누르기 시작
            }
            for (Presser presser : pressers) {
                presser.join(); // 모두 끝날 때까지 기다림
            }

            // then
            List<String> order = queue.order();
            assertEquals(PLAYERS, order.size(), round + "회차 줄 길이");
            assertEquals(PLAYERS, new HashSet<>(order).size(), round + "회차 중복");
        }
    }
}
