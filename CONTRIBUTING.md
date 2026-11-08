# 기여 가이드

> 작업을 시작하기 전에 꼭 읽어 주세요. 브랜치·커밋·PR·리뷰 규칙을 이 문서 한 곳에 모았어요.
> 규칙을 바꾸고 싶다면 이 문서를 수정하는 PR을 올려 주세요.

## 처음 세팅하기

### 1. JDK 17 이상

```bash
java -version
```

17 이상이면 돼요. 코드는 Java 17 문법까지만 써요. (`build.gradle`의 `release = 17`이 더 새로운 문법을 막아요.)

### 2. IntelliJ로 열기

편집·실행·커밋은 모두 **IntelliJ**(Community로 충분)에서 해요. 둘이 같은 IDE를 써야 포맷·실행 설정 차이로 쓸데없는 변경이 PR에 끼지 않아요.

1. File → Open → 저장소 루트 폴더 선택. `build.gradle`을 알아보고 Gradle 프로젝트로 열어요.
2. 오른쪽 아래 Gradle 동기화가 끝날 때까지 기다려요.

### 3. 서버 1 + 클라이언트 여러 개 실행

1. `ServerMain`의 초록 실행 버튼으로 서버를 먼저 켜요.
2. `ClientMain`을 한 번 실행한 뒤, 실행 설정(Run → Edit Configurations → ClientMain)에서 Modify options → **Allow multiple instances**를 켜요.
3. 이제 `ClientMain`을 누를 때마다 창이 하나씩 더 떠요. 통합 테스트는 서버 1 + 클라이언트 3개로 해요.

### IntelliJ에서 자주 쓰는 것

| 하고 싶은 일 | IntelliJ | 터미널 |
| --- | --- | --- |
| 서버 실행 | `ServerMain` 실행 버튼 | `./gradlew runServer` |
| 클라이언트 실행 | `ClientMain` 실행 버튼 (여러 번) | `./gradlew runClient` |
| 테스트 | 테스트 클래스 옆 실행 버튼 | `./gradlew test` |
| 전체 검사 (CI와 같음) | Gradle 창 → build | `./gradlew build` |
| 포맷 | `Ctrl+Alt+L` | — |

> PR 올리기 전에 `./gradlew build`가 통과하는지 확인해요. CI도 같은 명령을 돌려요.

## 코드 규칙

설계는 SOLID(SRP·OCP·LSP·ISP·DIP)를 따라요. 원칙별 적용과 이유는 [docs/github-plan.md 2.9절](docs/github-plan.md)에 있어요. 요약하면:

- **패키지 역할을 지켜요.** 소켓 코드는 `net`에만, 게임 규칙은 `game`에만, 화면은 `ui`에만 둬요. `game`은 소켓을 몰라요.
- **새 메시지 처리는 핸들러로 추가해요.** 큰 `switch`를 늘리지 않고, `MessageHandler`를 구현한 클래스를 만들어 `MessageDispatcher`에 등록해요.
- **같은 인터페이스의 구현은 같은 약속을 지켜요(LSP).** 예를 들어 채점기는 어떤 입력이 와도 예외 없이 `true`/`false`만 돌려줘요. 다른 구현으로 바꿔 끼워도 쓰는 쪽 코드가 그대로여야 해요.
- **인터페이스는 작게 둬요(ISP).** 쓰는 쪽이 필요 없는 메서드까지 알게 만들지 않아요. 기능이 늘면 기존 인터페이스를 키우지 말고 새로 나눠요.
- **숫자는 `GameConfig`에 모아요.** 시간·인원·점수를 코드 중간에 직접 쓰지 않아요.
- **공유 상태는 그 클래스 안에서 동기화해요.** `GameRoom`, `BellQueue`의 필드를 밖에서 직접 건드리지 않고 메서드로만 써요.
- **이름은 PRD 용어를 따라요.** 벨 대기열 → `BellQueue`, 초성 힌트 → `HintMaker`.
- **공개 메서드에는 Javadoc 한두 줄**로 무엇을, 어떤 조건에서 하는지 적어요.
- **`game` 패키지 클래스는 JUnit 테스트를 같이 써요.** 테스트 이름은 "~하면 ~된다", 본문은 `// given` · `// when` · `// then`으로 나눠요.

```java
@Test
@DisplayName("같은 사람이 벨을 두 번 누르면 한 번만 줄에 들어간다")
void buzzTwice() {
    // given
    BellQueue queue = new BellQueue();
    // when
    queue.add("이건");
    queue.add("이건");
    // then
    assertEquals(1, queue.size());
}
```

> 위 코드가 아직 없는 클래스나 패턴을 새로 들여오려면, 왜 필요한지 PR 본문에 적고 상대와 먼저 이야기해요.

## 작업 흐름

```
담당 이슈 확인 → 브랜치 만들기 → 작업·커밋 → PR 올리기 → 상대 리뷰 → Squash merge
```

1. **담당 이슈 확인**: 작업은 모두 이슈로 할당되어 있어요. 내 이슈가 아닌 작업을 하게 되면 담당자와 먼저 이야기해요.
2. **브랜치 만들기**: 항상 최신 `main`에서 만들어요. IntelliJ 오른쪽 아래 브랜치 이름 → `main` → Update, 그다음 New Branch로 `feat/#7-bell-queue`를 만들어요.
3. **커밋하고 push**: 커밋 컨벤션을 따라 커밋한 뒤 원격에 브랜치를 올려요.
4. **PR 올리기**: GitHub 웹(또는 IntelliJ의 Pull Requests 창)에서 PR을 만들고 상대에게 리뷰를 요청해요.

   <details><summary>터미널로 할 때</summary>

   ```bash
   git switch main
   git pull origin main
   git switch -c feat/#7-bell-queue
   git commit -m "feat: 벨 대기열 중복 방지 추가"
   git push -u origin feat/#7-bell-queue
   ```
   </details>

5. **머지 조건**을 만족하면 PR을 올린 사람이 직접 **Squash and merge** 해요.

## 형식 한눈에 보기

| 대상 | 형식 | 예시 |
| --- | --- | --- |
| 이슈 제목 | `[마일스톤] 할 일 (기능 ID)` | `[M2] 벨 대기열과 순서 표시 (D1~D3)` |
| 브랜치 | `타입/#이슈번호-설명` | `feat/#7-bell-queue` |
| 커밋 | `타입: 제목` | `feat: 벨 대기열 중복 방지 추가` |
| PR 제목 | `타입: #이슈번호 제목` | `feat: #7 벨 대기열과 순서 표시 구현` |

### 타입

브랜치, 커밋, PR 제목에서 모두 같은 타입을 써요.

| 타입 | 언제 |
| --- | --- |
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `design` | 화면 모양 변경 (색·글꼴·배치·이미지) |
| `refactor` | 동작은 그대로, 코드 구조 개선 |
| `style` | 코드 포맷만 변경 |
| `docs` | 문서 |
| `test` | 테스트 |
| `chore` | 빌드 설정, 문제 파일 등 기타 |

## 브랜치

- 설명은 **영어 소문자**와 `-`만 써요.
- 이슈 번호는 앞에 0을 붙이지 않아요. `#3`이지 `#03`이 아니에요.

## 커밋

```
타입: 제목

본문 (선택)
```

- 제목은 **한글로, 50자 이내**, 끝에 마침표를 찍지 않아요.
- 제목은 "~ 추가", "~ 수정"처럼 **명사형**으로 끝내요.
- 본문은 필요할 때만, **무엇을 했는지보다 왜 했는지**를 적어요.
- 커밋 하나에는 **하나의 의도**만 담아요. 머지할 때 하나로 합쳐지니 자주 커밋해도 괜찮아요.

## PR

- **PR 하나 = 이슈 하나**예요. 본문에 `Closes #번호`를 적으면 머지될 때 이슈가 닫혀요.
- 이슈가 커서 꼭 나눠야 할 때만 앞 PR에는 `Refs #번호`, 마지막 PR에 `Closes #번호`.
- 본문은 PR 템플릿을 채워요. 템플릿 항목이 **주간 보고서 항목**과 맞춰져 있어서, PR을 잘 쓰면 보고서가 거의 다 써져요.
- 변경은 **400줄 이내**를 권장해요.
- 화면이 바뀌면 **실행 화면 캡처**, 통신이 바뀌면 **서버 로그**를 붙여요.
- 아직 작업 중이면 **Draft PR**로 올려요.
- 둘 다 쓰는 파일(`MessageType`, `GameConfig`, `GameRoom`, `MessageDispatcher` 등록부)을 바꿀 때는 **그 변경만 담은 작은 PR**로 먼저 머지해요.
- AI를 썼다면 `ai_logs/<GitHub아이디>/YYYY-MM-DD-issue-N.md`에 기록하고 PR 본문에 링크해요. 양식은 [`ai_logs/TEMPLATE.md`](ai_logs/TEMPLATE.md).

## 리뷰

2명이라 리뷰어는 **항상 상대방**이에요. 리뷰는 상대 코드를 읽는 시간이기도 해요. 발표 때 둘 다 전체 흐름을 설명할 수 있어야 해요(PRD N10).

- 리뷰 댓글은 **해당 코드 줄**에 달고, 첫머리에 **추가 / 삭제 / 변경**을 적어요.
- 고칠 내용이 정해져 있으면 suggestion으로 달아요.
- 이해가 안 되는 부분은 그냥 넘기지 말고 질문해요.

## 머지 조건

- [ ] 상대방 승인(Approve)
- [ ] CI(`./gradlew build`) 통과
- [ ] 서버 1 + 클라이언트 여러 개로 직접 실행해서 확인 (통신·화면이 바뀐 경우)
- [ ] `main`과 충돌 없음

## 주간 보고서와 제출

- 마일스톤 M0~M5는 주차와 1:1이에요(이름 괄호 안이 주차). 주말에 그 주 마일스톤을 열면 내가 한 이슈·PR이 다 보여요.
- 막힌 순간에 바로 이슈 댓글로 현상·에러 로그를 남겨요. 보고서 5번 "문제 해결 과정"의 재료예요.
- 주말에 같이 30분 통합 테스트 (서버 1 + 클라이언트 3개, PRD 5절 시나리오). 찾은 문제는 `type: bug` 이슈로 등록해요.
- 제출용 프로젝트 폴더는 태그로 만들어요.

  ```bash
  git tag week11
  git push origin week11
  ```

  GitHub 저장소 → Tags → `week11` → Source code (zip).

## 충돌이 났을 때

- 해결한 뒤에는 **직접 실행해서 동작을 확인**하고 push해요.
- 상대 코드와 충돌했고 어떻게 고칠지 모르겠다면 혼자 판단하지 말고 같이 봐요.

## 하지 말아야 할 것

- `main`에 직접 push
- `git push --force` (내 브랜치에서 꼭 필요할 때만 `--force-with-lease`)
- `build/`, `out/`, `.idea/`, 제출용 `*.zip` 커밋
- 문제 파일에 실명·연락처 같은 실제 개인정보 넣기
- 이슈 없이 작업 시작
