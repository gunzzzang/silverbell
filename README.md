# 실버벨 (Silverbell)

골든벨 방식의 채팅 퀴즈 게임입니다. 네트워크프로그래밍 텀프로젝트로, 팀 실버벨(이건, 김주성)이 만듭니다.

문제가 뜨면 아는 사람이 벨을 울려 답할 기회를 잡고, 벨을 울린 순서대로 답합니다. 앞사람이 틀리면 다음 사람에게 차례가 넘어가고, 정답 판정과 점수는 서버가 관리합니다. 문제 사이에는 자유롭게 채팅합니다.

- 기획: [PRD (요구사항 명세서)](https://app.notion.com/p/408e56ed38a581c08a39e18c3dee1cc5) · [UI 명세서](https://app.notion.com/p/408e56ed38a581c6bf13df9cf63cf41b)
- 작업 규칙: [CONTRIBUTING.md](CONTRIBUTING.md)
- 일정·할 일: [마일스톤](https://github.com/gunzzzang/silverbell/milestones) · [이슈](https://github.com/gunzzzang/silverbell/issues)

## 구조

서버 1개 + 클라이언트 2~4개 (TCP 소켓). 서버를 먼저 실행하고 클라이언트가 IP로 접속합니다.

```
src/main/java/silverbell/
  common/          서버·클라이언트 공용 (메시지 형식, 설정 상수)
  server/
    net/           소켓 입출력만 (접속 받기, 클라이언트별 스레드, 보내기)
    game/          게임 규칙만, 소켓을 모름 (방, 벨 대기열, 점수, 문제, 힌트)
    game/answer/   채점 방식 (4지선다, 단답형)
    handler/       메시지 종류별 처리
  client/
    net/           서버 연결 (보내기·받기)
    ui/            화면 (타이틀, 대기실, 게임, 결과)
src/test/java/     JUnit 테스트 (main과 같은 패키지 구조)
questions/         문제 파일
```

설계 원칙과 코드 규칙은 [CONTRIBUTING.md 「코드 규칙」](CONTRIBUTING.md#코드-규칙)에 있습니다.

## 통신 프로토콜

한 줄에 메시지 하나, `타입|내용` 형식입니다. 값이 여러 개면 `;`로 구분하고, 채팅·문제처럼 사람이 쓴 글은 항상 마지막 값에 둡니다. 전체 목록은 PRD 9절, 코드는 `common/MessageType.java`에 있습니다.

```
CHAT|이건;안녕 ㅋㅋ|;ㅋ        → 닉네임 "이건", 메시지 "안녕 ㅋㅋ|;ㅋ"
```

## 문제 파일 형식

`questions/` 아래 txt 파일, 한 줄에 한 문제입니다.

```
유형|난이도|문제|보기1;보기2;보기3;보기4|정답
4지선다|1|TCP는 몇 계층 프로토콜인가?|물리;데이터링크;네트워크;전송|4
단답형|3|직각삼각형 세 변의 관계를 나타낸 정리는?||피타고라스의 정리/피타고라스 정리
```

- 난이도 1(쉬움)·2(보통)·3(어려움) = 기본 점수
- 단답형은 보기 칸을 비우고, 정답 후보는 `/`로 구분 (첫 번째가 초성 힌트에 쓰임)

## 실행 방법

JDK 17 이상이 필요합니다.

**IntelliJ**: 실행 목록에서 `ServerMain`을 먼저 실행하고, `ClientMain`을 여러 번 실행합니다. 실행 설정은 `.run/`에 들어 있고, `ClientMain`은 여러 창을 동시에 띄우도록 설정돼 있습니다.

**터미널** (저장소 루트에서):

```bash
./gradlew runServer      # 서버
./gradlew runClient      # 클라이언트 (터미널을 여러 개 열어서)
./gradlew test           # 테스트
```

Windows에서는 `./gradlew` 대신 `gradlew.bat`을 씁니다.
