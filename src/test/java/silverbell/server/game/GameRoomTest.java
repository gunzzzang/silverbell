package silverbell.server.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import silverbell.common.JoinFailReason;
import silverbell.common.Message;
import silverbell.common.MessageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GameRoomTest {

    private GameRoom gameRoom;
    private DummyBroadcaster broadcaster;

    static class DummyBroadcaster implements Broadcaster {
        // 멀티스레드 테스트 시 ConcurrentModificationException 방지를 위해 동기화 리스트 사용
        final List<Message> broadcastedMessages = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void broadcast(Message message) {
            broadcastedMessages.add(message);
        }

        @Override
        public void sendTo(String nickname, Message message) {
            // 단위 테스트에서는 전체 브로드캐스트 위주 검증
        }
    }

    @BeforeEach
    void setUp() {
        broadcaster = new DummyBroadcaster();
        gameRoom = new GameRoom(broadcaster);
    }

    @Test
    @DisplayName("첫 번째 접속자가 방장이 되고 joined 호출 시 USERS 메시지가 전달된다")
    void testFirstUserIsHostAndBroadcastsUsers() {
        // 1. 입장 시도 (tryJoin)
        assertNull(gameRoom.tryJoin("User1"));
        assertTrue(gameRoom.getPlayers().get(0).isHost());
        // tryJoin만 불렸을 때는 아직 USERS가 방송되지 않아야 함 (JOIN_OK 순서 보장)
        assertEquals(0, broadcaster.broadcastedMessages.size());

        // 2. JOIN_OK 이후 joined 호출
        gameRoom.joined("User1");
        assertEquals(1, broadcaster.broadcastedMessages.size());

        Message msg = broadcaster.broadcastedMessages.get(0);
        assertEquals(MessageType.USERS, msg.type());
        assertEquals("User1:HOST:NOT_READY", msg.body());

        // 3. 두 번째 유저 입장 시 GUEST로 추가됨
        assertNull(gameRoom.tryJoin("User2"));
        gameRoom.joined("User2");
        assertFalse(gameRoom.getPlayers().get(1).isHost());
        assertEquals("User1:HOST:NOT_READY;User2:GUEST:NOT_READY",
                broadcaster.broadcastedMessages.get(1).body());
    }

    @Test
    @DisplayName("닉네임 규칙(길이 1~10자, 공백·세미콜론·파이프·콜론 금지)을 어기면 실패 사유를 반환한다")
    void testInvalidNickname() {
        // 길이 위반 (공백 제거 후 0자 또는 11자 이상)
        assertEquals(JoinFailReason.INVALID_NICKNAME_LENGTH, gameRoom.tryJoin(null));
        assertEquals(JoinFailReason.INVALID_NICKNAME_LENGTH, gameRoom.tryJoin("   "));
        assertEquals(JoinFailReason.INVALID_NICKNAME_LENGTH, gameRoom.tryJoin("12345678901"));

        // 금지 문자 위반 (공백, ;, |, :)
        assertEquals(JoinFailReason.INVALID_NICKNAME_CHAR, gameRoom.tryJoin("User 1"));
        assertEquals(JoinFailReason.INVALID_NICKNAME_CHAR, gameRoom.tryJoin("User;1"));
        assertEquals(JoinFailReason.INVALID_NICKNAME_CHAR, gameRoom.tryJoin("User|1"));
        assertEquals(JoinFailReason.INVALID_NICKNAME_CHAR, gameRoom.tryJoin("주성:HOST"));

        // 앞뒤 공백은 trim 후 10자 이내면 정상 허용
        assertNull(gameRoom.tryJoin("  ValidUser  "));
    }

    @Test
    @DisplayName("중복된 닉네임으로 입장 시도 시 거부된다")
    void testDuplicateNickname() {
        assertNull(gameRoom.tryJoin("User1"));
        assertEquals(JoinFailReason.DUPLICATE_NICKNAME, gameRoom.tryJoin("User1"));
        assertEquals(JoinFailReason.DUPLICATE_NICKNAME, gameRoom.tryJoin("  User1  "));
    }

    @Test
    @DisplayName("최대 정원 초과 시 입장이 거부된다")
    void testMaxCapacity() {
        for (int i = 1; i <= silverbell.common.GameConfig.MAX_PLAYERS; i++) {
            assertNull(gameRoom.tryJoin("User" + i));
        }

        assertEquals(JoinFailReason.ROOM_FULL, gameRoom.tryJoin("UserExtra"));
    }

    @Test
    @DisplayName("게임 진행 중에는 입장이 거부된다")
    void testJoinWhileGaming() {
        gameRoom.setGaming(true);
        assertEquals(JoinFailReason.GAME_IN_PROGRESS, gameRoom.tryJoin("User1"));
    }

    @Test
    @DisplayName("방장이 퇴장하면 다음 참가자에게 방장이 위임되고 USERS가 전송된다")
    void testHostLeavesAndDelegates() {
        gameRoom.tryJoin("HostUser");
        gameRoom.joined("HostUser");
        gameRoom.tryJoin("GuestUser");
        gameRoom.joined("GuestUser");

        // 방장 퇴장
        gameRoom.left("HostUser");

        assertEquals(1, gameRoom.getPlayers().size());
        assertEquals("GuestUser", gameRoom.getPlayers().get(0).getNickname());
        assertTrue(gameRoom.getPlayers().get(0).isHost());

        Message lastMsg = broadcaster.broadcastedMessages.get(broadcaster.broadcastedMessages.size() - 1);
        assertEquals("GuestUser:HOST:NOT_READY", lastMsg.body());
    }

    @Test
    @DisplayName("여러 클라이언트가 동시에 입장해도 최대 4명까지만 정상 등록된다")
    void testConcurrentJoin() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger fullFailCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String name = "User" + i;
            executor.execute(() -> {
                try {
                    latch.await();
                    String reason = gameRoom.tryJoin(name);
                    if (reason == null) {
                        successCount.incrementAndGet();
                        gameRoom.joined(name);
                    } else if (JoinFailReason.ROOM_FULL.equals(reason)) {
                        fullFailCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // 10명 동시 요청 시 정확히 4명 성공, 6명 ROOM_FULL 실패
        assertEquals(4, successCount.get());
        assertEquals(6, fullFailCount.get());
        assertEquals(4, gameRoom.getPlayers().size());
        // 방장은 오직 1명만 존재해야 함
        assertEquals(1, gameRoom.getPlayers().stream().filter(GameRoom.Player::isHost).count());
    }
}