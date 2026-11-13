package silverbell.server.game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 문제 파일을 읽어 섞어 두고 앞에서부터 한 문제씩 꺼낸다. 다 쓰면 다시 섞는다.
 * 다 쓸 때까지 같은 문제가 다시 나오지 않는다. (PRD C1 문제 제시, R3 문제 파일, Q3 출제 방식)
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

                // 1. 칸 정확히 5개 검사
                String[] parts = line.split("\\|", -1);
                if (parts.length != 5) {
                    System.err.println("[QuestionDeck] 칸 수 오류 건너뜀 (Line " + lineNumber + "): " + line);
                    continue;
                }

                String type = parts[0].trim();
                String difficulty = parts[1].trim();
                String prompt = parts[2].trim();
                String choicesStr = parts[3].trim();
                String answerStr = parts[4].trim();

                // 2. 유형 검사 ("4지선다" / "단답형")
                if (!"4지선다".equals(type) && !"단답형".equals(type)) {
                    System.err.println("[QuestionDeck] 잘못된 유형 건너뜀 (Line " + lineNumber + "): " + line);
                    continue;
                }

                // 3. 난이도 검사 ("1", "2", "3")
                if (!"1".equals(difficulty) && !"2".equals(difficulty) && !"3".equals(difficulty)) {
                    System.err.println("[QuestionDeck] 잘못된 난이도 건너뜀 (Line " + lineNumber + "): " + line);
                    continue;
                }

                // 문제 내용 공백 검사
                if (prompt.isEmpty()) {
                    System.err.println("[QuestionDeck] 문제 내용 누락 건너뜀 (Line " + lineNumber + "): " + line);
                    continue;
                }

                List<String> choices = new ArrayList<>();
                List<String> answers = new ArrayList<>();

                // 4. 유형별 보기 및 정답 세부 검증
                if ("4지선다".equals(type)) {
                    String[] choiceArr = choicesStr.split(";", -1);
                    if (choiceArr.length != 4) {
                        System.err.println("[QuestionDeck] 4지선다 보기 개수 오류 건너뜀 (Line " + lineNumber + "): " + line);
                        continue;
                    }

                    boolean hasEmptyChoice = false;
                    for (String c : choiceArr) {
                        String trimmedChoice = c.trim();
                        if (trimmedChoice.isEmpty()) {
                            hasEmptyChoice = true;
                            break;
                        }
                        choices.add(trimmedChoice);
                    }
                    if (hasEmptyChoice) {
                        System.err.println("[QuestionDeck] 4지선다 빈 보기 포함 건너뜀 (Line " + lineNumber + "): " + line);
                        continue;
                    }

                    // 정답 1~4 검사
                    if (!"1".equals(answerStr) && !"2".equals(answerStr) && !"3".equals(answerStr) && !"4".equals(answerStr)) {
                        System.err.println("[QuestionDeck] 4지선다 정답 번호 오류 건너뜀 (Line " + lineNumber + "): " + line);
                        continue;
                    }
                    answers.add(answerStr);

                } else { // "단답형"
                    if (answerStr.isEmpty()) {
                        System.err.println("[QuestionDeck] 단답형 정답 누락 건너뜀 (Line " + lineNumber + "): " + line);
                        continue;
                    }

                    String[] answerArr = answerStr.split("/", -1);
                    boolean hasEmptyAnswer = false;
                    for (String a : answerArr) {
                        String trimmedAnswer = a.trim();
                        if (trimmedAnswer.isEmpty()) {
                            hasEmptyAnswer = true;
                            break;
                        }
                        answers.add(trimmedAnswer);
                    }
                    if (hasEmptyAnswer || answers.isEmpty()) {
                        System.err.println("[QuestionDeck] 단답형 빈 정답 후보 포함 건너뜀 (Line " + lineNumber + "): " + line);
                        continue;
                    }
                }

                Question q = new Question(type, difficulty, prompt, choices, answers);
                originalQuestions.add(q);
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
            Question copy = new Question(q.getType(), q.getDifficulty(), q.getPrompt(), q.getChoices(), q.getAnswers());
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