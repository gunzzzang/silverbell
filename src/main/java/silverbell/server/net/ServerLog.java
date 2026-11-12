package silverbell.server.net;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 서버 콘솔 로그. 받은·보낸 메시지를 시각과 함께 남겨 통합 테스트와 주간 보고서에 쓴다.
 *
 * <pre>[20:31:05.231] 받음 이건: BUZZ|</pre>
 *
 * <p>여러 스레드가 동시에 찍어도 한 줄이 섞이지 않게 {@code synchronized}로 한 줄씩 찍는다.
 */
public final class ServerLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private ServerLog() {
    }

    /** 시각을 붙여 한 줄 찍는다. */
    public static synchronized void info(String text) {
        System.out.println("[" + LocalTime.now().format(TIME) + "] " + text);
    }
}
