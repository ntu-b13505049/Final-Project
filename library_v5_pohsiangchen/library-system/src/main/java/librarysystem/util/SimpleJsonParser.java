package librarysystem.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimpleJsonParser {
    private final String source;
    private int index;

    private SimpleJsonParser(String source) {
        this.source = source == null ? "" : source;
        this.index = 0;
    }

    public static Object parse(String jsonText) {
        SimpleJsonParser parser = new SimpleJsonParser(jsonText);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw parser.error("JSON 結尾後仍有多餘內容");
        }
        return value;
    }

    private Object parseValue() {
        skipWhitespace();
        if (isEnd()) {
            throw error("JSON 提前結束");
        }

        char ch = source.charAt(index);
        return switch (ch) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't' -> parseLiteral("true", Boolean.TRUE);
            case 'f' -> parseLiteral("false", Boolean.FALSE);
            case 'n' -> parseLiteral("null", null);
            default -> {
                if (ch == '-' || Character.isDigit(ch)) {
                    yield parseNumber();
                }
                throw error("無法解析的 JSON 值開頭：" + ch);
            }
        };
    }

    private Map<String, Object> parseObject() {
        expect('{');
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        skipWhitespace();

        if (peek('}')) {
            index++;
            return map;
        }

        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            Object value = parseValue();
            map.put(key, value);
            skipWhitespace();

            if (peek('}')) {
                index++;
                return map;
            }
            expect(',');
        }
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();

        if (peek(']')) {
            index++;
            return list;
        }

        while (true) {
            list.add(parseValue());
            skipWhitespace();

            if (peek(']')) {
                index++;
                return list;
            }
            expect(',');
        }
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();

        while (!isEnd()) {
            char ch = source.charAt(index++);
            if (ch == '"') {
                return sb.toString();
            }
            if (ch == '\\') {
                if (isEnd()) {
                    throw error("字串跳脫字元不完整");
                }
                char escaped = source.charAt(index++);
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        sb.append(escaped);
                        break;
                    case 'b':
                        sb.append('\b');
                        break;
                    case 'f':
                        sb.append('\f');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'u':
                        sb.append(parseUnicodeEscape());
                        break;
                    default:
                        throw error("不支援的跳脫字元：\\" + escaped);
                }
            } else {
                sb.append(ch);
            }
        }

        throw error("字串未正確結束");
    }

    private char parseUnicodeEscape() {
        if (index + 4 > source.length()) {
            throw error("Unicode 跳脫字元不完整");
        }
        String hex = source.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw error("無效的 Unicode 跳脫字元：\\u" + hex);
        }
    }

    private Object parseLiteral(String literal, Object value) {
        if (!source.startsWith(literal, index)) {
            throw error("無效的 JSON 常值，預期：" + literal);
        }
        index += literal.length();
        return value;
    }

    private Number parseNumber() {
        int start = index;

        if (source.charAt(index) == '-') {
            index++;
        }

        consumeDigits();

        boolean isDecimal = false;
        if (!isEnd() && source.charAt(index) == '.') {
            isDecimal = true;
            index++;
            consumeDigits();
        }

        if (!isEnd() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
            isDecimal = true;
            index++;
            if (!isEnd() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                index++;
            }
            consumeDigits();
        }

        String token = source.substring(start, index);
        try {
            return isDecimal ? Double.parseDouble(token) : Long.parseLong(token);
        } catch (NumberFormatException e) {
            throw error("無法解析數字：" + token);
        }
    }

    private void consumeDigits() {
        int digitStart = index;
        while (!isEnd() && Character.isDigit(source.charAt(index))) {
            index++;
        }
        if (digitStart == index) {
            throw error("數字格式不合法");
        }
    }

    private void expect(char expected) {
        skipWhitespace();
        if (isEnd() || source.charAt(index) != expected) {
            throw error("預期字元 '" + expected + "'");
        }
        index++;
    }

    private boolean peek(char ch) {
        return !isEnd() && source.charAt(index) == ch;
    }

    private void skipWhitespace() {
        while (!isEnd() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
    }

    private boolean isEnd() {
        return index >= source.length();
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + "（位置 " + index + "）");
    }
}
