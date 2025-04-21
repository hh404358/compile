/**
 * @Author: hahaha
 * @Package:PACKAGE_NAME
 * @Project:word_translator
 * @name:word_translator
 * @Date:2025/4/21
 * @Time:15:36
 * @Filename:word_translator
 **/

import java.io.*;
import java.util.*;
/**
 * @Author: hahaha
 * @Package:com.ljr.translator
 * @Project:translator
 * @name:Word_Analysis
 * @Date:2025/4/21
 * @Time:15:15
 * @Filename:Word_Analysis
 **/


// 词法单元类型枚举
enum TokenType {
    KEYWORD, IDENTIFIER, CONSTANT, OPERATOR,
    DELIMITER, STRING_LIT, CHAR_LIT, ERROR
}

// 词法单元类
class Token {
    String value;
    TokenType type;
    int line;
    int column;

    public Token(String value, TokenType type, int line, int column) {
        this.value = value;
        this.type = type;
        this.line = line;
        this.column = column;
    }
}

// 错误信息类
class ErrorInfo {
    int line;
    int column;
    String message;

    public ErrorInfo(int line, int column, String message) {
        this.line = line;
        this.column = column;
        this.message = message;
    }
}

// 词典类
class WordList {
    public static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "int", "float", "char", "string", "if", "else",
            "while", "for", "return", "void"
    ));

    public static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
            "+", "-", "*", "/", "=", "==", "!=", "<", ">", "<=", ">=",
            "&&", "||", "!", "++", "--", "+=", "-=", "*=", "/="
    ));

    public static final Set<String> DELIMITERS = new HashSet<>(Arrays.asList(
            ";", ",", "(", ")", "{", "}", "[", "]", "#"
    ));
}

// 字符读取器类
class CharReader {
    private BufferedReader reader;
    private Deque<Character> buffer = new ArrayDeque<>();
    private int line = 1;
    private int column = 0;

    public CharReader(String input) throws IOException {
        this.reader = new BufferedReader(new StringReader(input));
    }

    public char nextChar() throws IOException {
        if (!buffer.isEmpty()) {
            char ch = buffer.removeFirst();
            column++;
            return ch;
        }

        int ch = reader.read();
        if (ch == -1) return '\0';

        if (ch == '\n') {
            line++;
            column = 0;
        } else {
            column++;
        }
        return (char) ch;
    }

    public void unread(char ch) {
        buffer.addFirst(ch);
        column--;
    }

    public int getLine() { return line; }
    public int getColumn() { return column; }

    public void close() throws IOException {
        reader.close();
    }
}
enum State {
    START, IN_IDENT, IN_NUM, IN_OPERATOR,
    IN_COMMENT_SINGLE, IN_COMMENT_MULTI,
    IN_STRING, IN_CHAR, IN_ERROR
}
// 词法分析器主类
public class WordAnalysis {
    private List<Token> tokens = new ArrayList<>();
    private List<ErrorInfo> errors = new ArrayList<>();
    private LinkedHashSet<String> symbolTable = new LinkedHashSet<>();
    private CharReader reader;

    public void analyze(String input) throws IOException {

        reader = new CharReader(input);
        State currentState = State.START;
        StringBuilder currentToken = new StringBuilder();
        int tokenStartLine = 1;
        int tokenStartColumn = 1;

        char ch;
        while ((ch = reader.nextChar()) != '\0') {
            switch (currentState) {
                case START:
                    if (Character.isWhitespace(ch)) {
                        tokenStartLine = reader.getLine();
                        tokenStartColumn = reader.getColumn();
                        continue;
                    }
                    currentToken.setLength(0);
                    tokenStartLine = reader.getLine();
                    tokenStartColumn = reader.getColumn();

                    if (Character.isLetter(ch) || ch == '_') {
                        currentToken.append(ch);
                        currentState = State.IN_IDENT;
                    } else if (Character.isDigit(ch)) {
                        currentToken.append(ch);
                        currentState = State.IN_NUM;
                    } else if (ch == '/') {
                        char next = reader.nextChar();
                        if (next == '/') {
                            currentState = State.IN_COMMENT_SINGLE;
                        } else if (next == '*') {
                            currentState = State.IN_COMMENT_MULTI;
                        } else {
                            reader.unread(next);
                            currentToken.append(ch);
                            currentState = State.IN_OPERATOR;
                        }
                    } else if (ch == '"') {
                        currentToken.append(ch);
                        currentState = State.IN_STRING;
                    } else if (ch == '\'') {
                        currentToken.append(ch);
                        currentState = State.IN_CHAR;
                    } else if (WordList.OPERATORS.contains(String.valueOf(ch))) {
                        currentToken.append(ch);
                        currentState = State.IN_OPERATOR;
                    } else if (WordList.DELIMITERS.contains(String.valueOf(ch))) {
                        tokens.add(new Token(String.valueOf(ch), TokenType.DELIMITER,
                                tokenStartLine, tokenStartColumn));
                    } else {
                        errors.add(new ErrorInfo(tokenStartLine, tokenStartColumn,
                                "非法字符: " + ch));
                    }
                    break;

                case IN_IDENT:
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        currentToken.append(ch);
                    } else {
                        processToken(currentToken.toString(), tokenStartLine, tokenStartColumn);
                        reader.unread(ch);
                        currentState = State.START;
                    }
                    break;

                case IN_NUM:
                    if (Character.isDigit(ch) || ch == '.') {
                        currentToken.append(ch);
                    } else {
                        processToken(currentToken.toString(), tokenStartLine, tokenStartColumn);
                        reader.unread(ch);
                        currentState = State.START;
                    }
                    break;

                case IN_OPERATOR:
                    String possibleOp = currentToken.toString() + ch;
                    if (WordList.OPERATORS.contains(possibleOp)) {
                        currentToken.append(ch);
                        tokens.add(new Token(possibleOp, TokenType.OPERATOR,
                                tokenStartLine, tokenStartColumn));
                        currentState = State.START;
                    } else {
                        tokens.add(new Token(currentToken.toString(), TokenType.OPERATOR,
                                tokenStartLine, tokenStartColumn));
                        reader.unread(ch);
                        currentState = State.START;
                    }
                    break;

                case IN_COMMENT_SINGLE:
                    if (ch == '\n') currentState = State.START;
                    break;

                case IN_COMMENT_MULTI:
                    if (ch == '*' && reader.nextChar() == '/') {
                        currentState = State.START;
                    }
                    break;

                case IN_STRING:
                    currentToken.append(ch);
                    if (ch == '"') {
                        tokens.add(new Token(currentToken.toString(), TokenType.STRING_LIT,
                                tokenStartLine, tokenStartColumn));
                        currentState = State.START;
                    } else if (ch == '\\') {
                        char next = reader.nextChar();
                        currentToken.append(next);
                    }
                    break;

                case IN_CHAR:
                    currentToken.append(ch);
                    if (ch == '\'') {
                        tokens.add(new Token(currentToken.toString(), TokenType.CHAR_LIT,
                                tokenStartLine, tokenStartColumn));
                        currentState = State.START;
                    } else if (ch == '\\') {
                        char next = reader.nextChar();
                        currentToken.append(next);
                    }
                    break;
            }
        }

        // 处理文件末尾未完成的token
        if (currentState != State.START) {
            processToken(currentToken.toString(), tokenStartLine, tokenStartColumn);
            errors.add(new ErrorInfo(tokenStartLine, tokenStartColumn, "未闭合的token"));
        }

        reader.close();
    }

    private void processToken(String token, int line, int column) {
        TokenType type = determineTokenType(token);

        // 错误类型检测
        if (type == TokenType.ERROR) {
            String errorDetail = determineErrorType(token);
            errors.add(new ErrorInfo(line, column, errorDetail));
        }

        // 合法标识符检测（需排除数字开头的情况）
        if (type == TokenType.IDENTIFIER && isInvalidIdentifier(token)) {
            errors.add(new ErrorInfo(line, column, "非法标识符: " + token));
            type = TokenType.ERROR;
        }

        tokens.add(new Token(token, type, line, column));

        // 仅合法标识符加入符号表
        if (type == TokenType.IDENTIFIER && !isInvalidIdentifier(token)) {
            symbolTable.add(token);
        }
    }
    // 新增错误类型判断方法
    private String determineErrorType(String token) {
        // 数字开头的标识符错误
        if (token.matches("^\\d.*")) {
            return String.format("错误的 %s（非法标识符，不能以数字开头）", token);
        }

        // 非法数字常量格式
        if (token.matches("^\\d+\\.\\d+\\..*")) {
            return String.format("错误的 %s（非法数字格式，多个小数点）", token);
        }

        // 非法科学计数法
        if (token.matches("^\\d+e\\D.*") || token.matches("^\\d+\\.\\d+e\\D.*")) {
            return String.format("错误的 %s（非法科学计数法）", token);
        }

        // 其他未识别类型
        return String.format("错误的 %s（无法识别的符号）", token);
    }

    // 增强的标识符合法性验证
    private boolean isInvalidIdentifier(String token) {
        // 检查是否以数字开头
        if (token.matches("^\\d.*")) return true;

        // 检查是否包含非法字符
        if (!token.matches("[a-zA-Z_][a-zA-Z0-9_]*")) return true;

        // 检查是否为关键字
        return WordList.KEYWORDS.contains(token);
    }

    private TokenType determineTokenType(String token) {
        // 优先检测非法标识符
        if (isInvalidIdentifier(token)) {
            // 数字开头的特殊处理
            if (token.matches("^\\d.*")) return TokenType.ERROR;
        }
        if (isInvalidNumber(token)) {
            return TokenType.ERROR;
        }
        if (WordList.KEYWORDS.contains(token)) return TokenType.KEYWORD;
        if (WordList.OPERATORS.contains(token)) return TokenType.OPERATOR;
        if (WordList.DELIMITERS.contains(token)) return TokenType.DELIMITER;
        if (token.startsWith("\"") && token.endsWith("\"")) return TokenType.STRING_LIT;
        if (token.startsWith("'") && token.endsWith("'")) return TokenType.CHAR_LIT;
        if (token.matches("\\d+(\\.\\d+)?")) return TokenType.CONSTANT;
        if (token.matches("[a-zA-Z_][a-zA-Z0-9_]*")) return TokenType.IDENTIFIER;
        return TokenType.ERROR;
    }

    // 数字常量合法性验证
    private boolean isInvalidNumber(String token) {
        // 检测十六进制格式
        if (token.matches("^0[xX][0-9a-fA-F]+$")) return false;

        // 检测二进制格式
        if (token.matches("^0[bB][01]+$")) return false;

        // 常规数字格式检查
        try {
            Double.parseDouble(token);
            return false;
        } catch (NumberFormatException e) {
            // 包含非法字符的情况
            if (token.matches(".*[^0-9.eE+-].*")) return true;

            // 多个小数点
            if (token.indexOf('.') != token.lastIndexOf('.')) return true;

            // 科学计数法错误
            if (token.contains("e") || token.contains("E")) {
                String[] parts = token.split("[eE]");
                if (parts.length != 2) return true;
                if (!parts[1].matches("[+-]?\\d+")) return true;
            }
            return true;
        }
    }
    public String getResults() {
        StringBuilder result = new StringBuilder();

        // 处理Tokens
        result.append("Tokens:\n");
        for (Token token : tokens) {
            result.append(String.format("(%s, %s) 行%d列%d\n",
                    token.value, token.type.toString(), token.line, token.column));
        }

        // 处理错误信息
        if (!errors.isEmpty()) {
            result.append("\n错误信息:\n");
            for (ErrorInfo error : errors) {
                result.append(String.format("行%d列%d: %s\n",
                        error.line, error.column, error.message));
            }
        }

        // 处理符号表
        result.append("\n符号表:\n");
        for (String symbol : symbolTable) {
            result.append(symbol).append("\n");
        }

        return result.toString();
    }


//    public static void main(String[] args) {
//        try {
//            WordAnalysis analyzer = new WordAnalysis();
//            analyzer.analyze("input.txt");
//            analyzer.printResults();
//        } catch (IOException e) {
//            System.err.println("文件读取错误: " + e.getMessage());
//        }
//    }
}

