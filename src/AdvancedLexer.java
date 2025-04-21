/**
 * @Author: hahaha
 * @Package:PACKAGE_NAME
 * @Project:word_translator
 * @name:Advanced
 * @Date:2025/4/21
 * @Time:18:53
 * @Filename:Advanced
 **/
import java.util.*;


// 词法单元类型枚举
enum TokenType {
    KEYWORD,        // 关键字
    IDENTIFIER,     // 标识符
    NUMERIC_CONST,  // 数值常量
    STRING_CONST,   // 字符串常量
    OPERATOR,       // 运算符
    DELIMITER,      // 分隔符
    ERROR           // 错误标记
}

// 词法单元类
class Token {
    public final TokenType type;
    public final String value;
    public final int line;
    public final int position;

    public Token(TokenType type, String value, int line, int position) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.position = position;
    }

    @Override
    public String toString() {
        return String.format("<%s, %s> row:%d,column:%d",  value, type.name(), line, position);
    }
}
// 辅助类用于传递索引值
class IntWrapper {
    public int value;
    public IntWrapper(int value) { this.value = value; }
}
public class AdvancedLexer {
    private int currentLine = 1;
    private int currentPos = 1;
    private final SymbolTable symbolTable = new SymbolTable();
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final List<Token> tokens = new ArrayList<>();

    // 关键字集合（使用Set加速查找）
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "WHILE", "IF", "ELSE", "BREAK", "CONTINUE",
            "INT", "DOUBLE", "FLOAT", "CHAR", "UNSIGNED",
            "RETURN", "GOTO", "THEN", "STATIC", "NEW",
            "CASE", "SWITCH", "DEFAULT", "TRUE", "FALSE",
            "BOOL", "FOR"
    ));

    // 运算符映射表
    private static final Map<String, String> OPERATORS = new HashMap<>();
    static {
        OPERATORS.put("+", "ADD");
        OPERATORS.put("-", "SUB");
        OPERATORS.put("*", "MUL");
        OPERATORS.put("/", "DIV");
        OPERATORS.put("=", "ASSIGN");
        OPERATORS.put("==", "EQ");
        OPERATORS.put("!=", "NEQ");
        OPERATORS.put("<", "LT");
        OPERATORS.put("<=", "LE");
        OPERATORS.put(">", "GT");
        OPERATORS.put(">=", "GE");
        OPERATORS.put("&&", "AND");
        OPERATORS.put("||", "OR");
        OPERATORS.put("!", "NOT");
        OPERATORS.put("&", "BIT_AND");
        OPERATORS.put("|", "BIT_OR");
        OPERATORS.put("^", "BIT_XOR");
        OPERATORS.put("%", "MOD");
        OPERATORS.put("++", "INC");
        OPERATORS.put("--", "DEC");
    }

    // 分隔符集合
    private static final Set<Character> DELIMITERS = new HashSet<>(Arrays.asList(
            '(', ')', '{', '}', '[', ']', ';', ',', '.', ':'
    ));


    public List<Token> analyze(String input) {
        input = preprocessInput(input);
        StringBuilder lexeme = new StringBuilder();
        int length = input.length();
        IntWrapper index = new IntWrapper(0);
        while (index.value < input.length()) {
            char c = input.charAt(index.value);

            if (Character.isDigit(c)) {
                processNumericLiteral(input, index);
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                int start = index.value;
                while (index.value < length && (Character.isLetterOrDigit(input.charAt(index.value)) || input.charAt(index.value) == '_')) {
                    lexeme.append(input.charAt(index.value++));
                }
                String identifier = lexeme.toString();
                lexeme.setLength(0);

                if (KEYWORDS.contains(identifier.toUpperCase())) {
                    tokens.add(new Token(TokenType.KEYWORD, identifier, currentLine, start));
                } else {
                    tokens.add(new Token(TokenType.IDENTIFIER, identifier, currentLine, start));
                    symbolTable.addIdentifier(identifier);
                }
                currentPos += index.value - start;
                continue;
            }
            // 处理数值常量
            if (Character.isDigit(c)) {
                int start = index.value;
                boolean isHex = false;
                boolean isOctal = false;

                if (c == '0' && index.value+1 < length) {
                    char next = input.charAt(index.value+1);
                    if (next == 'x' || next == 'X') {
                        isHex = true;
                        index.value += 2;
                    } else if (Character.isDigit(next)) {
                        isOctal = true;
                        index.value++;
                    }
                }

                while (index.value < length && Character.isDigit(input.charAt(index.value))) {
                    lexeme.append(input.charAt(index.value++));
                }

                // 处理十六进制
                if (isHex) {
                    while (index.value < length && isHexDigit(input.charAt(index.value))) {
                        lexeme.append(input.charAt(index.value++));
                    }
                }

                tokens.add(new Token(TokenType.NUMERIC_CONST, lexeme.toString(), currentLine, start));
                lexeme.setLength(0);
                currentPos += index.value - start;
                continue;
            }

            // 处理字符串常量
            if (c == '"') {
                int start = index.value++;
                while (index.value < length && input.charAt(index.value) != '"') {
                    lexeme.append(input.charAt(index.value++));
                }
                if (index.value >= length) {
                    errorHandler.addError(currentLine, "Unclosed string literal");
                } else {
                    index.value++;
                    tokens.add(new Token(TokenType.STRING_CONST, lexeme.toString(), currentLine, start));
                    lexeme.setLength(0);
                }
                currentPos += index.value - start;
                continue;
            }

            // 处理运算符
            if (OPERATORS.containsKey(Character.toString(c))) {
                int start = index.value;
                // 检测多字符运算符
                if (index.value+1 < length) {
                    String dualOp = input.substring(index.value, index.value+2);
                    if (OPERATORS.containsKey(dualOp)) {
                        tokens.add(new Token(TokenType.OPERATOR, dualOp, currentLine, start));
                        index.value += 2;
                        currentPos += 2;
                        continue;
                    }
                }
                tokens.add(new Token(TokenType.OPERATOR, Character.toString(c), currentLine, start));
                index.value++;
                currentPos++;
                continue;
            }

            // 处理分隔符
            if (DELIMITERS.contains(c)) {
                tokens.add(new Token(TokenType.DELIMITER, Character.toString(c), currentLine, index.value));
                index.value++;
                currentPos++;
                continue;
            }

            // 处理换行和空格
            if (c == '\n') {
                currentLine++;
                currentPos = 1;
                index.value++;
            } else if (Character.isWhitespace(c)) {
                currentPos++;
                index.value++;
            } else {
                // 无法识别的字符
                errorHandler.addError(currentLine, "Illegal character: " + c);
                tokens.add(new Token(TokenType.ERROR, Character.toString(c), currentLine, index.value));
                index.value++;
                currentPos++;
            }
        }
        return tokens;
    }

    // 预处理逻辑（移除注释、统一大小写等）
    private String preprocessInput(String input) {
        return input.replaceAll("//.*", "")  // 移除单行注释
                .replaceAll("/\\*.*?\\*/", "") // 移除多行注释
                .toUpperCase(); // 统一为小写处理
    }

    private void processNumericLiteral(String input, IntWrapper index) {
        StringBuilder buffer = new StringBuilder();
        int startLine = currentLine;
        int startPos = currentPos;
        boolean hasError = false;

        // 收集所有连续数字
        while (index.value < input.length() && Character.isDigit(input.charAt(index.value))) {
            buffer.append(input.charAt(index.value++));
            currentPos++;
        }

        // 检查后续字符是否为字母（非法标识符情况）
        if (index.value < input.length() && Character.isLetter(input.charAt(index.value))) {
            hasError = true;
            buffer.append(input.charAt(index.value++));
            currentPos++;

            // 继续收集直到非字母数字
            while (index.value < input.length() &&
                    (Character.isLetterOrDigit(input.charAt(index.value)) ||
                            input.charAt(index.value) == '_')) {
                buffer.append(input.charAt(index.value++));
                currentPos++;
            }

            errorHandler.addError(currentLine, buffer.toString());
            tokens.add(new Token(TokenType.ERROR, buffer.toString(), startLine, startPos));
            return;
        }

        // 正常数值常量处理
        tokens.add(new Token(TokenType.NUMERIC_CONST, buffer.toString(), startLine, startPos));
    }

    private boolean isHexDigit(char c) {
        return Character.digit(c, 16) != -1;
    }

    public String getAnalysisResult() {
        StringBuilder sb = new StringBuilder();

        // 输出词法单元
        sb.append("Tokens:\n");
        tokens.forEach(t -> sb.append(t).append("\n"));

        // 输出符号表
        sb.append("\nSymbol Table:\n").append(symbolTable);

        // 输出错误信息
        if (!errorHandler.isEmpty()) {
            sb.append("\nErrors:\n").append(errorHandler);
        }

        return sb.toString();
    }
}

class SymbolTable {
    private final Map<String, Integer> identifiers = new HashMap<>();
    private int nextId = 0;

    public void addIdentifier(String name) {
        if (!identifiers.containsKey(name)) {
            identifiers.put(name, nextId++);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        identifiers.forEach((k, v) -> sb.append(v).append("\t").append(k).append("\n"));
        return sb.toString();
    }
}

class ErrorHandler {
    private final Map<Integer, List<String>> errors = new HashMap<>();

    public void addError(int line, String message) {
        errors.computeIfAbsent(line, k -> new ArrayList<>()).add(message);
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        errors.forEach((line, messages) -> {
            messages.forEach(msg -> sb.append("Line ").append(line).append(": ").append(msg).append("\n"));
        });
        return sb.toString();
    }
}