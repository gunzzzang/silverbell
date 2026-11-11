# CLAUDE.md — 실버벨 코딩 가이드 (Claude용)

> 이 저장소에서 코드를 쓰는 Claude가 매번 먼저 읽는 문서예요. 사람이 읽어도 돼요.
> 작업 규칙(브랜치·커밋·PR)은 [`CONTRIBUTING.md`](CONTRIBUTING.md), 구조·프로토콜은 [`README.md`](README.md)가 기준이에요. 이 문서는 그 위에 **"수업 코드 스타일에 맞추기"** 규칙을 더해요.

## 0. 가장 중요한 원칙

네트워크프로그래밍 텀프로젝트라서 **수업에서 배운 코드에서 크게 벗어나지 않아요.** 채점 기준이 수업 내용이고, 이건·김주성 둘 다 발표 때 코드를 직접 설명할 수 있어야 해요.

1. **수업 예제에 있는 방식이 있으면 그 방식으로 써요.** 클래스 구조, 스트림 종류, 스레드 만드는 법, 리스너 다는 법까지.
2. **수업에 없는 기술은 "진짜 필요할 때만"** 써요. 쓰면 ① 코드 주석에 왜 필요한지 한 줄, ② PR 「핵심 코드와 설계 이유」에 설명, ③ 아래 [5절 표](#5-수업-밖-기술-목록)에 추가해요.
3. **수업 코드의 버그는 따라 하지 않아요.** [4절](#4-수업-코드에서-따라-하면-안-되는-것)에 정리돼 있어요.
4. **이미 있는 뼈대(`Message`, `MessageDispatcher`, `Broadcaster`, `GameConfig`, `MainFrame`)는 그대로 써요.** 수업 코드가 `split`·`switch`로 하던 일을 이 클래스들이 대신하는 것뿐이에요. 뼈대를 수업 코드 모양으로 되돌리지 않아요.

> 수업 코드를 "그대로 복사"가 아니라 **"수업 코드의 모양을 우리 패키지에 나눠 담는"** 거예요. 각 파일이 어느 수업 예제에서 왔는지는 2절 표를 봐요.
> 수업 채팅 예제 원본은 [`class-example/`](class-example/)에 손대지 않고 들어 있어요(#4). 모양이 헷갈리면 먼저 열어 봐요.

## 1. 수업에서 배운 것 (11주차까지)

| 주제 | 수업 예제 | 핵심 |
| --- | --- | --- |
| 1:1 소켓 | `ServerEx`/`ClientEx`, `TestServer`/`TestClient` 1~3 | `ServerSocket` → `accept()`, `new Socket(host, port)`, 한 줄씩 번갈아 주고받기, `"bye"`/`"끝"`으로 종료 |
| 스트림 종류 | `TestClient31~36`, `TestServer3~5`, `TestKeyboard1~4` | `InputStream`(byte) → `InputStreamReader`(char) → `BufferedReader`(`readLine`), `Scanner`, `PrintWriter`, `DataInput/OutputStream`(`readUTF`/`writeUTF`) |
| 이벤트 리스너 5가지 | `ex1`~`ex5` | 외부 클래스+static / 외부 클래스+생성자 전달 / **내부 클래스** / **익명 클래스** / 자기 클래스가 `implements` |
| 멀티 채팅 (GUI) | `JavaChatServer`, `JavaChatClientMain`, `JavaChatClientView` | 접속 받는 스레드(`AcceptServer`) + 사용자마다 스레드(`UserService`) + `Vector`로 관리 + `WriteOne`/`WriteAll`, `/login`·`/exit`·`/list`·`/to` 명령어 |
| 멀티 채팅 (콘솔 서버) | `ServerM`, `MessengerMulti` | `static ArrayList`로 스레드 관리, `finally`에서 `closeResources()`, `volatile boolean running` 종료 플래그, `WindowAdapter.windowClosing`에서 `cleanup()` |
| 키 이벤트·포커스 | `KeyInputExample`, `FocusExample`, `FocusAdapterExample` | `keyPressed`/`keyReleased`/`keyTyped`, `setFocusable(true)`, `requestFocusInWindow()`, `KeyAdapter`·`FocusAdapter`, `setVisible` 직후 포커스 요청이 실패할 수 있어서 `SwingUtilities.invokeLater`로 미룸 |
| 게임 루프 | `ShootingGame`, `ShootingGame1` | `javax.swing.Timer(ms, this)` + `actionPerformed`에서 갱신 → `repaint()`, `boolean keys[256]`로 동시 키 입력, `paintComponent`, `Iterator.remove()`로 돌면서 지우기, `Rectangle.intersects` |

## 2. 우리 파일 ↔ 수업 예제

코드를 쓰기 전에 오른쪽 예제의 모양을 먼저 떠올려요.

| 우리 파일 | 따라갈 수업 예제 | 맞출 것 |
| --- | --- | --- |
| `server/ServerMain` | `JavaChatServer` 생성자·서버 시작 버튼 | 객체를 한 번씩 만들고 핸들러 등록, 포트 열기 |
| `server/net/SocketServer` | `JavaChatServer.AcceptServer`, `ServerM.main` | `extends Thread`, `while` 안에서 `accept()` → `ClientHandler` 생성 → 목록에 추가 → `start()` |
| `server/net/ClientHandler` | `JavaChatServer.UserService`, `ServerM`의 `ServerThread` | `extends Thread`, 생성자에서 소켓·스트림 준비, `run()`에서 `readUTF` 반복, `send` = `WriteOne`, 끊기면 `finally`에서 목록 제거 + 닫기 |
| `server/net/ClientRegistry` (`Broadcaster` 구현) | `UserVec`, `WriteAll`, `/to`의 이름 찾기 반복문 | `Vector`에 담고, 돌며 한 명씩 `send` |
| `server/handler/*Handler` | `UserService.run`의 `switch` 각 `case` | `case` 하나 = 핸들러 클래스 하나. 내용은 `case` 안에 쓰던 코드와 비슷하게 |
| `client/net/ServerConnection` | `JavaChatClientView`의 소켓 부분 + `ListenNetwork` + `SendMessage`, `MessengerMulti.cleanup` | 받는 스레드는 내부 클래스 `extends Thread`(`readUTF` 반복), 보내기 메서드, 닫힘 플래그와 `close()` |
| `client/net/MessageRouter` | `UserService.run`의 `switch`를 클라이언트 쪽에 둔 것 | 화면이 받고 싶은 종류를 등록, 화면 고치기는 EDT로 |
| `client/ConsoleClient` (테스트용) | `ClientEx`, `TestKeyboard3` | 키보드 한 줄 읽어 그대로 보내기 |
| `client/ui/*Screen` | `JavaChatClientMain`/`View`의 화면 구성 | `JLabel`·`JTextField`·`JButton`·`JScrollPane`+`JTextArea`, 버튼과 입력창 Enter에 같은 리스너 |
| 벨 누르기 키 | `FocusExample`, `KeyInputExample` | `KeyAdapter.keyPressed`, 포커스 받을 패널에 `setFocusable(true)` |
| 화면의 남은 시간 표시 | `ShootingGame`의 `javax.swing.Timer` | 1초마다 `actionPerformed`에서 숫자 줄이고 다시 그리기 |

## 3. 코드 스타일 규칙

### 3-1. 소켓 입출력

- **스트림은 수업 멀티 채팅 예제처럼 `DataInputStream`/`DataOutputStream`의 `readUTF()`/`writeUTF()`로 통일해요** (#4에서 정함). 메시지 하나 = `writeUTF` 한 번이라 줄바꿈으로 나눌 필요가 없고, 인코딩이 고정돼 PC·JDK가 달라도 한글이 안 깨져요.

  ```java
  in = new DataInputStream(socket.getInputStream());   // 소켓 입력 스트림
  out = new DataOutputStream(socket.getOutputStream()); // 소켓 출력 스트림

  out.writeUTF(message.toLine()); // 메시지 하나 전송
  out.flush();

  String line = in.readUTF();     // 메시지 하나 받기. 상대가 닫으면 EOFException
  ```

- `BufferedReader.readLine()`·`PrintWriter`(1:1 예제 방식)와 섞어 쓰지 않아요. 한쪽이 `writeUTF`, 다른 쪽이 `readLine`이면 메시지가 깨져요. 키보드 입력(`ConsoleClient`)만 `TestKeyboard3`처럼 `BufferedReader(new InputStreamReader(System.in))`를 써요.
- 소켓 스트림은 수업처럼 `getInputStream()`에 바로 씌워요. `BufferedInputStream`/`BufferedOutputStream`을 중간에 끼우지 않아요(`writeUTF`는 한 번에 써서 필요 없어요).
- `writeUTF`는 64KB가 넘으면 `UTFDataFormatException`을 던져요. 이것도 `IOException`이라 그냥 잡으면 긴 채팅 하나에 연결이 끊겨요. **따로 먼저 잡아서 그 메시지만 버려요**(수업 예제에 없는 함정).
- `send` 메서드는 `synchronized`로 해요. 수업 코드에는 없지만 벨·타이머·채팅 스레드가 한 사람에게 동시에 보내면 메시지가 섞여요.

### 3-2. 스레드

- 스레드는 수업처럼 **`extends Thread` + `run()` 오버라이드**로 만들어요(`AcceptServer`, `UserService`, `ListenNetwork`, `ServerThread`). 클래스 하나가 하는 일이 분명할 때 이게 가장 읽기 쉬워요.
- 한 번만 쓰는 짧은 스레드는 `MessengerMulti`처럼 `new Thread(new Runnable() { ... })`도 돼요.
- 멈춰야 하는 반복 스레드는 `MessengerMulti`처럼 **`volatile boolean running`** 플래그 + `cleanup()`에서 `running = false` 후 소켓 닫기.
- `ExecutorService`, `CompletableFuture`, `ConcurrentHashMap` 같은 `java.util.concurrent`는 기본으로 안 써요. 공유 상태는 `synchronized`로 지켜요(CONTRIBUTING: 공유 상태는 그 클래스 안에서 동기화).
- 접속자 목록은 수업처럼 **`Vector`**(`JavaChatServer.UserVec`)를 써요. 돌면서 보낼 때는 목록 전체를 잠가요. 이렇게 하면 도는 중에 누가 나가도 목록이 줄지 않아서 수업 예제의 「다음 사람 건너뛰기」 버그가 안 생겨요.

  ```java
  synchronized (handlers) {           // 도는 중에 누가 나가서 목록이 바뀌지 않게
      for (int i = 0; i < handlers.size(); i++) {
          handlers.get(i).send(message);
      }
  }
  ```

### 3-3. 예외 처리와 종료

- 수업 `ServerM`처럼 **`try` → `catch (IOException e)` → `finally`에서 목록 제거 + `closeResources()`** 순서로 써요.
- `closeResources()`는 `ServerM` 모양 그대로: `if (x != null) x.close();`, 소켓은 `!s.isClosed()`까지 확인.
- 한 클라이언트의 예외가 서버 전체를 멈추면 안 돼요. `run()` 안에서 잡고 그 스레드만 끝내요.
- 잘못된 메시지(`Message.parse`의 `IllegalArgumentException`)는 잡고 **그 줄만 무시**해요. 연결은 유지해요.
- 클라이언트는 `System.exit()`로 끝내지 않아요(수업 코드 주석에도 "테스트용"이라고 적혀 있음). `QUIT` 보내고 연결 닫기. 서버는 시작할 때 포트를 못 열면만 `System.exit(1)`.
- 서버 로그는 `System.out.println`으로 콘솔에. 받은·보낸 메시지에 시각을 붙여요(PR·보고서에 붙일 서버 로그).

### 3-4. 화면 (Swing)

- **화면 배치는 수업처럼 `setLayout(null)` + `setBounds(x, y, w, h)`** 를 기본으로 해요(`JavaChatClientMain`/`View`, WindowBuilder로 만든 모양). 화면 바꾸기는 이미 있는 `MainFrame`의 `CardLayout`을 써요. 단순한 영역은 수업 `ex1~5`·`MessengerMulti`처럼 `BorderLayout`도 돼요.
- 컴포넌트 이름은 수업 접두어를 따라요: `txtNickname`, `txtAnswer`, `btnSend`, `btnBell`, `lblTimer`, `textArea`, `scrollPane`.
- 채팅창은 수업 그대로 `JScrollPane` + `JTextArea`(`setEditable(false)`), 추가할 때 `append` 후 `setCaretPosition(textArea.getText().length())`로 맨 아래로.
- 입력창은 수업처럼 **버튼과 입력창 Enter에 같은 리스너**를 달고, 보낸 뒤 `setText("")` + `requestFocus()`.
- **받는 스레드에서 화면을 고칠 때는 `SwingUtilities.invokeLater`로 감싸요.** 수업 `JavaChatClientView`는 받는 스레드에서 바로 `append`했지만, `FocusAdapterExample`에서 배운 것처럼 Swing은 EDT에서 고쳐야 안전해요. (`MessageListener` 주석에도 적혀 있음)
- 시간 표시처럼 화면에서 반복되는 일은 `ShootingGame`의 **`javax.swing.Timer`** 를 써요. `actionPerformed`가 EDT에서 불려서 `invokeLater`가 필요 없어요.

### 3-5. 이벤트 리스너 (ex1~ex5 중 고르기)

| 상황 | 방식 | 수업 예제 |
| --- | --- | --- |
| 여러 컴포넌트가 같은 동작 (버튼 + Enter) | **내부 클래스** `class SendAction implements ActionListener` | `ex3`, `JavaChatClientView.Myaction` |
| 한 군데서만 쓰는 짧은 동작 | **익명 클래스** `new ActionListener() { ... }` | `ex4`, `JavaChatServer`의 시작 버튼 |
| 메서드 하나만 필요한 리스너 | **Adapter 익명 클래스** `new KeyAdapter() { ... }`, `new WindowAdapter() { ... }` | `FocusAdapterExample`, `MessengerMulti` |
| 패널 자체가 키·타이머를 다 받는 화면 | 자기 클래스가 `implements` | `ex5`, `ShootingGame` |

- `ex1`(static 필드로 접근)은 쓰지 않아요.
- 리스너 이름은 `Myaction` 대신 하는 일을 적어요: `SendAction`, `BellAction`, `ConnectAction`.
- 람다는 **한 줄짜리 등록**에만 써요: `SwingUtilities.invokeLater(() -> ...)`(수업 `FocusAdapterExample`에 있음), `router.on(MessageType.CHAT, m -> chatPanel.append(m))`, `dispatcher.register(...)`, 테스트. 두 줄 이상이면 내부 클래스나 익명 클래스로 써서 수업 코드와 모양을 맞춰요.
- 메서드 참조(`Foo::bar`), `java.util.function`(`Consumer` 등)은 쓰지 않아요. 콜백이 필요하면 `MessageListener`처럼 우리 인터페이스를 만들어요.

### 3-6. 이름·주석·포맷

- 이름은 Java 표준 camelCase로 써요. 수업 코드의 `AppendText`, `WriteOne`, `UserName`, `client_socket`, `ip_addr`는 각각 `appendText`, `send`, `nickname`, `clientSocket`, `ipAddress`처럼 바꿔요.
- 변수 이름은 수업에서 쓰던 걸 살려요: `socket`, `serverSocket`, `in`/`out`, `msg`, `line`. (`listener`는 `ConnectionListener`·`MessageListener`와 헷갈려서 `ServerSocket`에 쓰지 않아요.)
- 주석은 두 종류를 같이 써요.
  - 공개 클래스·메서드 위: Javadoc 한두 줄 (CONTRIBUTING 규칙)
  - 소켓·스레드·동기화 줄 끝: **수업처럼 한국어 `//` 주석으로 그 줄이 하는 일** (`// 클라이언트로부터 연결 요청 대기`, `// 버퍼를 비워야 실제로 나간다`). 발표·보고서 때 이 주석으로 설명해요.
  - `if`·`return` 같은 뻔한 줄에는 주석을 달지 않아요.
- import는 하나씩 명시해요(`java.io.*` 같은 `*` 안 씀). 포맷은 IntelliJ `Ctrl+Alt+L` 기본(공백 4칸).
- 숫자는 `GameConfig`에 모아요. 수업 코드처럼 `9999`, `"localhost"`를 코드 중간에 쓰지 않아요.

## 4. 수업 코드에서 따라 하면 안 되는 것

| 수업 코드 | 문제 | 우리는 |
| --- | --- | --- |
| `JavaChatServer`: `switch (args[1])` | 빈 메시지면 `"[이름]"`만 남아 `ArrayIndexOutOfBoundsException` → `IOException`이 아니라 안 잡히고 그 사용자 스레드가 죽음 | `Message.parse` + `MessageDispatcher` (핸들러는 `values(n)` 길이 확인) |
| `TestServer`·`ServerEx`: `inputMessage.equals(...)` | 상대가 끊으면 `readLine()`이 `null` → `NullPointerException` | 우리는 `readUTF`라 끊기면 `EOFException` → `catch`에서 반복 종료 |
| `writeUTF`를 그냥 `catch (IOException)` | 64KB 넘는 메시지 하나에 연결이 끊김 (`UTFDataFormatException`) | 먼저 따로 잡아 그 메시지만 버림 |
| `UserService.WriteOne` 실패 → `logout()` → 목록에서 제거 | `WriteAll`의 `for (i < size)` 도중 목록이 줄어서 다음 사람을 건너뜀 | 보내기 실패는 표시만 하고, 제거는 그 사람의 `run()` `finally`에서 한 번만 |
| `UserService`: 생성자에서 `readUTF()`로 `/login` 받기 | 생성자는 `AcceptServer` 스레드에서 돌아서, 한 명이 이름을 안 보내면 다른 사람 접속도 멈춤 | `JOIN` 받기는 `ClientHandler.run()` 안에서 |
| `logout()`이 `catch` 두 곳에서 불릴 수 있음 | 퇴장 메시지가 두 번 나갈 수 있음 | 퇴장 처리는 `finally` 한 곳 |
| `JavaChatServer`: `new ServerSocket` 실패해도 `AcceptServer` 시작 | `socket`이 `null`이라 `accept()`에서 무한 예외 | 포트 열기 실패면 로그 남기고 서버 시작 중단 |
| `client_socket`을 필드로 둠 | 여러 스레드가 같은 필드를 덮어씀 (주석에도 지역변수로 해도 된다고 적힘) | `accept()` 결과는 지역 변수 |
| `ServerEx`의 `finally`에서 `socket.close()` | `accept` 전에 실패하면 `socket`이 `null` → NPE | `if (socket != null)` (`ServerM.closeResources` 방식) |
| 받는 스레드에서 바로 `textArea.append` | EDT 밖에서 Swing 수정 | `SwingUtilities.invokeLater` |
| `msg.contains("/exit")`면 `System.exit(0)` | 채팅에 "/exit"가 들어가도 꺼짐, 서버에 퇴장이 안 감 | `QUIT` 메시지 + `cleanup()` |
| `BUF_LEN` 같은 안 쓰는 상수, 안 쓰는 `Scanner` | 읽는 사람이 헷갈림 | 쓰지 않는 건 만들지 않음 |

## 5. 수업 밖 기술 목록

이미 뼈대에 들어가 있어서 그대로 쓰는 것과, 새로 들인 것을 적어요. **새로 쓰면 이 표에 한 줄 추가**해요.

| 기술 | 어디 | 왜 필요한지 |
| --- | --- | --- |
| `enum` + `EnumMap` | `MessageType`, `MessageDispatcher` | 메시지 종류를 문자열 오타 없이 다루고, 큰 `switch` 대신 핸들러 등록(OCP) |
| `interface`로 의존 끊기 | `Broadcaster`, `MessageListener`, `MessageHandler`, `AnswerChecker` | `game`이 소켓을, `net`이 화면을 모르게(DIP), 소켓 없이 JUnit 테스트 |
| `Objects.requireNonNull`, `final class` 불변 객체 | `Message` | 여러 스레드가 같이 써도 안전 |
| `instanceof` 패턴 (Java 16) | `Message.equals` | 짧게 쓰려고. 다른 데서는 안 늘림 |
| `CardLayout` | `MainFrame` | 화면마다 창을 새로 띄우지 않고 한 창에서 전환 |
| 람다 (한 줄 등록만) | `invokeLater`, `router.on`, `dispatcher.register`, 테스트 | 수업 `FocusAdapterExample`과 같은 범위. 3-5절 |
| `synchronized` 메서드·블록 | `send`, 목록 순회, `ClientRegistry`, `GameRoom`, `BellQueue` | 수업은 동기화 없이 썼지만, 벨 순서(N1)·동시 전송에서 실제로 꼬임 |
| `socket.connect(주소, 시간 제한)` | `ServerConnection`, `ConsoleClient` | 서버가 없거나 IP가 틀리면 `new Socket(host, port)`는 오래 멈춤. 정해진 시간 안에 실패를 알림(N5) |
| `setTcpNoDelay(true)` | 서버·클라이언트 소켓 | 벨처럼 작은 메시지를 모으지 않고 바로 보냄(N1 벨 공정성) |
| `LocalTime` + `DateTimeFormatter` | `ServerLog` | 로그에 시각 붙이기(#4 완료 기준, 보고서용) |
| `AutoCloseable` | `SocketServer`, `ServerConnection` | JUnit에서 서버·연결을 켰다 끄기 |
| JUnit 5, Gradle | `src/test`, `build.gradle` | `game` 패키지 단위 테스트, CI |

**아직 정하지 않은 것** (정하면 위 표로 옮겨요)

- **서버 쪽 타이머**(문제 20초, 답변 10초, 초성 힌트 시점 — #11): 수업에는 화면용 `javax.swing.Timer`만 있었어요. 후보는 ① `extends Thread` + `Thread.sleep` + `running` 플래그(수업 스레드 범위 안), ② `javax.swing.Timer`. `GameRoom` 담당(라인 A)과 정해요.

**기본으로 쓰지 않는 것**: `java.util.concurrent` 전부(`ExecutorService`, `ConcurrentHashMap`, `CopyOnWriteArrayList`, `Atomic*`), `java.util.function`·메서드 참조, `ObjectOutputStream` 직렬화, JSON 라이브러리, NIO, 외부 라이브러리, Stream API, 여러 줄짜리 람다. 이걸로 할 일은 `Vector`·`synchronized`·`volatile`·우리 인터페이스로 해요.

## 6. Claude가 코딩할 때 순서

1. 이 문서 2절에서 **어느 수업 예제를 따라갈지** 먼저 정해요.
2. 해당 이슈 번호와 클래스 Javadoc의 「할 일」을 확인해요. 담당 라인(A 김주성 / B 이건)도 확인해요.
3. 브랜치·커밋·PR은 [`CONTRIBUTING.md`](CONTRIBUTING.md) 형식 그대로. 커밋 제목은 한글 명사형.
4. 수업 밖 기술을 쓰게 되면 멈추고 이건에게 이유를 말한 뒤 써요. 쓰면 5절 표에 추가.
5. AI 사용 기록은 `ai_logs/gunzzzang/YYYY-MM-DD-issue-N.md` ([양식](ai_logs/TEMPLATE.md)).
6. PR 전에 `./gradlew build` 통과 확인. 통신이 바뀌면 서버 1 + 클라이언트 여러 개로 확인한 로그를 남겨요.
7. 이건이 설명할 수 있어야 해요. PR 「핵심 코드와 설계 이유」에 **"수업 예제 ○○의 이 부분을 이렇게 옮겼다 / 이 부분은 이래서 바꿨다"** 형태로 적어요.
