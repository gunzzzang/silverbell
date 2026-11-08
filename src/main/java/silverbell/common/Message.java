package silverbell.common;

import java.util.Objects;

/**
 * 한 줄 문자열 메시지와 객체 사이의 변환. 형식은 {@code 타입|값1;값2;...}이다. (PRD 9절)
 *
 * <p>자르는 규칙을 이 클래스 한 곳에 모은다. 서버·클라이언트가 각자 {@code split}을 쓰면
 * 채팅에 {@code |}나 {@code ;}가 들어갔을 때 서로 다르게 잘라서 메시지가 깨진다.
 *
 * <pre>
 * Message.of(MessageType.CHAT, "이건", "안녕 ;)").toLine()  →  "CHAT|이건;안녕 ;)"
 * Message.parse("CHAT|이건;안녕 ;)").values(2)              →  ["이건", "안녕 ;)"]
 * </pre>
 *
 * <p>불변 객체라서 여러 스레드가 같이 써도 안전하다.
 */
public final class Message {

    private static final String TYPE_SEPARATOR = "|";
    private static final String VALUE_SEPARATOR = ";";

    private final MessageType type;
    private final String body;

    private Message(MessageType type, String body) {
        this.type = Objects.requireNonNull(type, "type");
        this.body = Objects.requireNonNull(body, "body");
    }

    /**
     * 보낼 메시지를 만든다. 사람이 쓴 글(채팅, 문제 등)은 반드시 마지막 값으로 넣는다.
     *
     * @throws IllegalArgumentException 마지막이 아닌 값에 {@code ;}가 있거나, 어느 값에든 줄바꿈이 있을 때
     */
    public static Message of(MessageType type, String... values) {
        for (int i = 0; i < values.length; i++) {
            String value = Objects.requireNonNull(values[i], "values[" + i + "]");
            if (value.contains("\n") || value.contains("\r")) {
                throw new IllegalArgumentException("메시지 값에 줄바꿈을 넣을 수 없다: values[" + i + "]");
            }
            boolean isLast = i == values.length - 1;
            if (!isLast && value.contains(VALUE_SEPARATOR)) {
                throw new IllegalArgumentException("마지막이 아닌 값에 ';'를 넣을 수 없다: values[" + i + "]");
            }
        }
        return new Message(type, String.join(VALUE_SEPARATOR, values));
    }

    /**
     * 받은 한 줄을 메시지로 바꾼다. 첫 {@code |} 앞이 타입, 나머지 전부가 내용이다.
     *
     * @throws IllegalArgumentException 모르는 타입일 때. 받는 쪽은 이 예외를 잡고 그 줄을 무시한다.
     */
    public static Message parse(String line) {
        Objects.requireNonNull(line, "line");
        String[] parts = line.split("\\" + TYPE_SEPARATOR, 2);
        MessageType type = MessageType.valueOf(parts[0]);
        String body = parts.length > 1 ? parts[1] : "";
        return new Message(type, body);
    }

    /**
     * 내용을 값 {@code count}개로 자른다. 마지막 값은 나머지 전부라서 안에 {@code ;}가 있어도 그대로 남는다.
     * 값이 몇 개인지는 {@link MessageType}의 주석을 보고 받는 쪽이 정한다.
     *
     * @param count 꺼낼 값 개수. 0 이하이면 개수 제한 없이 전부 자른다(목록 메시지용).
     */
    public String[] values(int count) {
        return body.split(VALUE_SEPARATOR, count > 0 ? count : -1);
    }

    /** 자르지 않은 내용 전체. 값이 하나뿐인 메시지에 쓴다. */
    public String body() {
        return body;
    }

    public MessageType type() {
        return type;
    }

    /** 소켓으로 보낼 한 줄. 줄바꿈은 보내는 쪽이 붙인다. */
    public String toLine() {
        return type.name() + TYPE_SEPARATOR + body;
    }

    @Override
    public String toString() {
        return toLine();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message other)) {
            return false;
        }
        return type == other.type && body.equals(other.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, body);
    }
}
