package silverbell.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Question {
    private final String type;          // "4지선다" 또는 "단답형"
    private final String difficulty;    // "1", "2", "3" 난이도
    private final String prompt;        // 문제 내용
    private final List<String> choices; // 보기 목록 (4지선다: 4개, 단답형: 빈 리스트)
    private List<String> answers;       // 정답 목록 (4지선다: ["1"~"4"], 단답형: ["서브넷 마스크", "서브넷마스크"])

    public Question(String type, String difficulty, String prompt, List<String> choices, List<String> answers) {
        this.type = type;
        this.difficulty = difficulty;
        this.prompt = prompt;
        this.choices = choices != null ? new ArrayList<>(choices) : new ArrayList<>();
        this.answers = answers != null ? new ArrayList<>(answers) : new ArrayList<>();
    }

    /**
     * 4지선다 문제인 경우 보기를 섞고, 섞인 위치에 맞춰 정답 번호(1~4)를 갱신합니다.
     */
    public void shuffleChoices() {
        if (!"4지선다".equals(type) || choices.size() != 4 || answers.isEmpty()) {
            return;
        }

        int originalCorrectIndex = Integer.parseInt(answers.get(0)) - 1;
        if (originalCorrectIndex < 0 || originalCorrectIndex >= choices.size()) {
            return;
        }

        // 기존 정답 보기 텍스트 보존
        String correctAnswerText = choices.get(originalCorrectIndex);

        // 보기 섞기
        Collections.shuffle(choices);

        // 섞인 보기에 맞춰 정답 번호 재설정 (1-based index)
        int newCorrectIndex = choices.indexOf(correctAnswerText);
        this.answers = new ArrayList<>(Collections.singletonList(String.valueOf(newCorrectIndex + 1)));
    }

    public String getType() { return type; }
    public String getDifficulty() { return difficulty; }
    public String getPrompt() { return prompt; }
    public List<String> getChoices() { return Collections.unmodifiableList(choices); }
    public List<String> getAnswers() { return Collections.unmodifiableList(answers); }
}