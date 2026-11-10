package silverbell.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

import silverbell.common.GameConfig;

/**
 * 테스트용 콘솔 클라이언트. 터미널에 친 줄을 <b>그대로</b> 서버에 보내고, 받은 줄을 그대로 찍는다.
 * 화면(#6)이 생기기 전 접속 테스트와, 일부러 엉뚱한 메시지를 보내는 N7 검증(#16·#21)에 쓴다.
 *
 * <p>{@code ServerConnection}은 올바른 {@code Message}만 보내므로, 깨진 줄도 보낼 수 있게 소켓을 직접 쓴다.
 *
 * <pre>
 * JOIN|이건            입장
 * CHAT|안녕            채팅
 * SCORE|이건;999       가짜 점수 (서버가 무시해야 함)
 * (빈 줄), 아무말       형식이 틀린 줄 (서버가 무시해야 함)
 * /burst 20 CHAT|동시  같은 줄을 번호를 붙여 20번 빠르게 보냄
 * /quit                종료
 * </pre>
 *
 * <p>실행: IntelliJ 실행 목록 {@code ConsoleClient}(여러 개 동시 실행 가능), 또는 {@code ./gradlew -q --console=plain runConsole}.
 * 인자로 {@code 주소 포트}를 줄 수 있다(기본 127.0.0.1, {@value GameConfig#DEFAULT_PORT}).
 */
public final class ConsoleClient {

    private ConsoleClient() {
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : GameConfig.DEFAULT_PORT;

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), GameConfig.CONNECT_TIMEOUT_MS);
        } catch (IOException e) {
            System.out.println("서버에 접속할 수 없어요 (" + host + ":" + port + "): " + e.getMessage());
            return;
        }
        System.out.println("접속했어요 " + host + ":" + port + " · 보낼 줄을 입력하세요 (예: JOIN|이건, CHAT|안녕, /burst 20 CHAT|동시, /quit)");

        DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        Thread reader = new Thread(() -> printReceived(in), "console-reader");
        reader.setDaemon(true);
        reader.start();

        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = keyboard.readLine()) != null) {
            if (line.equals("/quit")) {
                break;
            }
            try {
                if (line.startsWith("/burst ")) {
                    burst(out, line);
                } else {
                    send(out, line);
                }
            } catch (IOException e) {
                System.out.println("보내기 실패: " + e.getMessage());
                break;
            }
        }
        socket.close();
    }

    /** {@code /burst 20 CHAT|동시} → {@code CHAT|동시 1} ~ {@code CHAT|동시 20}을 쉬지 않고 보낸다. */
    private static void burst(DataOutputStream out, String command) throws IOException {
        String[] parts = command.split(" ", 3);
        if (parts.length < 3) {
            System.out.println("사용법: /burst 횟수 보낼줄");
            return;
        }
        int count;
        try {
            count = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            System.out.println("횟수는 숫자로 써 주세요: /burst 20 CHAT|동시");
            return;
        }
        for (int i = 1; i <= count; i++) {
            send(out, parts[2] + " " + i);
        }
        System.out.println("→ " + count + "줄 보냄");
    }

    private static void send(DataOutputStream out, String line) throws IOException {
        out.writeUTF(line);
        out.flush();
    }

    private static void printReceived(DataInputStream in) {
        try {
            while (true) {
                System.out.println("← " + in.readUTF());
            }
        } catch (IOException e) {
            System.out.println("서버 연결이 끊겼어요. 엔터를 누르면 끝나요.");
        }
    }
}
