package silverbell.server.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 문제 파일을 읽어 섞어 두고 앞에서부터 한 문제씩 꺼낸다. 다 쓰면 다시 섞는다.
 * 다 쓸 때까지 같은 문제가 다시 나오지 않는다. (PRD C1 문제 제시, R3 문제 파일, Q3 출제 방식)
 *
 * <p>할 일 (#8 · 라인 A, 네트워크 연결은 #11): 파일 읽기({@code 유형|난이도|문제|보기|정답}), 형식이 틀린 줄 건너뛰기,
 * 섞기, 꺼내기. 4지선다는 보기 순서도 섞는다.
 */
public class QuestionDeck {
    private final List<Question> originalQuestions = new ArrayList<>();
    private final List<Question> currentDeck = new ArrayList<>();
    private int currentIndex = 0;

    /**
     * InputStream에서 문제 파일을 읽어 덱을 구성합니다.
     * 형식이 틀리거나 잘못된 줄은 건너뛰고 오류 줄 번호를 출력합니다.
     */
    public void loadQuestions(InputStream inputStream) throws IOException {
        originalQuestions.clear();
        currentDeck.clear();
        currentIndex = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // 빈 줄이거나 주석(#)인 경우 건너뛰기
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                try {
                    // 형식: 유형|난이도|문제|보기1;보기2;보기3;보기4|정답
                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 5) {
                        System.err.println("[QuestionDeck] 형식이 틀린 줄 건너뜀 (Line " + lineNumber + "): " + line);
                        continue;
                    }

                    String type = parts[0].trim();
                    String difficulty = parts[1].trim();
                    String prompt = parts[2].trim();
                    String choicesStr = parts[3].trim();
                    String answer = parts[4].trim();

                    List<String> choices = new ArrayList<>();
                    if (!choicesStr.isEmpty()) {
                        choices = Arrays.asList(choicesStr.split(";"));
                    }

                    Question q = new Question(type, difficulty, prompt, choices, answer);
                    originalQuestions.add(q);
                } catch (Exception e) {
                    System.err.println("[QuestionDeck] 파싱 예외 발생 (Line " + lineNumber + "): " + line);
                }
            }
        }

        resetDeck();
    }

    /**
     * 덱을 새로 구성하고 보기를 섞은 뒤, 전체 문제 순서를 무작위로 섞습니다.
     */
    private void resetDeck() {
        currentDeck.clear();
        for (Question q : originalQuestions) {
            Question copy = new Question(q.getType(), q.getDifficulty(), q.getPrompt(), q.getChoices(), q.getAnswer());
            copy.shuffleChoices(); // 4지선다인 경우 보기 및 정답 번호 섞기
            currentDeck.add(copy);
        }
        Collections.shuffle(currentDeck); // 전체 문제 순서 섞기
        currentIndex = 0;
    }

    /**
     * 문제를 한 개씩 꺼냅니다. 다 쓰면 자동으로 다시 섞어 덱을 재구성합니다.
     */
    public Question drawNext() {
        if (currentDeck.isEmpty()) {
            throw new IllegalStateException("출제 가능한 문제가 없습니다.");
        }

        if (currentIndex >= currentDeck.size()) {
            resetDeck(); // 다 썼을 때 다시 섞기
        }

        return currentDeck.get(currentIndex++);
    }

    public int totalLoadedCount() {
        return originalQuestions.size();
    }
}
