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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// 词法单元类型枚举
enum TokenType {
    KEYWORD,        // 关键字
    IDENTIFIER,     // 标识符
    NUMERIC_CONST,  // 数值常量
    STRING_CONST,   // 字符串常量
    OPERATOR,       // 运算符
    DELIMITER,      // 分隔符
    CHAR_CONST, ERROR           // 错误标记
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
        return String.format("<%s, %s>",  value, type.name());
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
            // 处理数值常量
            if (Character.isDigit(c)) {
                int start = index.value;
                boolean isHex = false;
                boolean isOctal = false;
                boolean isDecimal = false;

                if (c == '0' && index.value+1 < length) {
                    char next = input.charAt(index.value+1);
                    if (next == 'x' || next == 'X') {// 0x 0X 开头 十六进制 0-9 A-F 超出这个范围报错
                        isHex = true;
                        index.value += 2;
                    }else if(next >= '0' && next <= '7'){// 八进制数以0开头 使用0-7表示数值
                        isOctal = true;
                        lexeme.append('0');
                        index.value++;
                    } else if (next=='.') {
                        isDecimal = true;
                        lexeme.append(c);
                        index.value++;
                    } else{
                        tokens.add(new Token(TokenType.ERROR, Character.toString(c), currentLine, index.value));
                        index.value++;
                        currentPos++;
                        continue;
                    }
                }else if(c != '0' && index.value+1 < length){
                    char next = input.charAt(index.value+1);

                    if (next=='.'){
                        isDecimal = true;
                        lexeme.append(c);
                        index.value++;
                    } else if(!Character.isDigit(next)){
                        tokens.add(new Token(TokenType.ERROR, Character.toString(c), currentLine, index.value));
                        index.value++;
                        currentPos++;
                        continue;
                    }
                }

                while (index.value < length && Character.isDigit(input.charAt(index.value))) {
                    lexeme.append(input.charAt(index.value++));
                }

                // 读取完现有数字 停滞在非数字 检查是否是 .
                if(input.charAt(index.value)=='.') isDecimal = true;

                // 处理小数
                if (isDecimal){
                    lexeme.append(input.charAt(index.value));
                    index.value++;
                    while(index.value<length&&Character.isDigit(input.charAt(index.value))){
                        lexeme.append(input.charAt(index.value++));
                    }
                    if(input.charAt(index.value)=='.') {
                        // 读到没有 . 为止 避免 3...14情况
                        while(index.value<length&&input.charAt(index.value)=='.'){
                            lexeme.append(input.charAt(index.value++));
                        }
                        // 处理小数点后数字
                        while (index.value < length && Character.isDigit(input.charAt(index.value))) {
                            lexeme.append(input.charAt(index.value++));
                        }
                        //然后报错？
                        errorHandler.addError(currentLine, currentPos,"Invalid numeric format: " + lexeme.toString());
                        tokens.add(new Token(TokenType.ERROR, lexeme.toString(), currentLine, index.value));
                        index.value++;
                        currentPos++;
                        continue;
                    }
                }

                Integer decimalValue = 0;
                // 处理十六进制
                if (isHex) {
                    while (index.value < length && isHexDigit(input.charAt(index.value))) {
                        lexeme.append(input.charAt(index.value++));
                    }
                    try {
                        decimalValue = Integer.parseInt(lexeme.toString(),16);
                        tokens.add(new Token(TokenType.NUMERIC_CONST, decimalValue.toString(), currentLine, start));
                    }catch (NumberFormatException e){
                        errorHandler.addError(currentLine, currentPos,"Invalid 16 numeric format: " + lexeme.toString());
                        tokens.add(new Token(TokenType.ERROR, lexeme.toString(), currentLine, index.value));
                        index.value++;
                    }
                }else if(isOctal){
                    while (index.value < length && isOctalDigit(input.charAt(index.value))) {
                        lexeme.append(input.charAt(index.value++));
                    }
                    try {
                        decimalValue = Integer.parseInt(lexeme.toString(),8);
                        tokens.add(new Token(TokenType.NUMERIC_CONST, decimalValue.toString(), currentLine, start));
                    }catch (NumberFormatException e){
                        errorHandler.addError(currentLine, currentPos,"Invalid 8 numeric format: " + lexeme.toString());
                        tokens.add(new Token(TokenType.ERROR, lexeme.toString(), currentLine, index.value));
                        index.value++;
                    }
                }else  {// 处理指数（仅限十进制）
                        if (index.value < length && (input.charAt(index.value) == 'e' || input.charAt(index.value) == 'E')) {
                            lexeme.append(input.charAt(index.value++));

                            // 处理符号
                            if (index.value < length && (input.charAt(index.value) == '+' || input.charAt(index.value) == '-')) {
                                lexeme.append(input.charAt(index.value++));
                            }

                            // 检查指数部分是否为数字
                            if (index.value >= length || !Character.isDigit(input.charAt(index.value))) {
                                errorHandler.addError(currentLine, currentPos,"Invalid exponent in numeric constant");
                                tokens.add(new Token(TokenType.ERROR, lexeme.toString(), currentLine, start));
                                lexeme.setLength(0);
                                continue;
                            }

                            // 读取指数数字
                            while (index.value < length && Character.isDigit(input.charAt(index.value))) {
                                lexeme.append(input.charAt(index.value++));
                            }
                        }
                        tokens.add(new Token(TokenType.NUMERIC_CONST, lexeme.toString(), currentLine, index.value));
                    }
                currentPos += index.value - start;
                continue;

            }

            if(c=='.'&& index.value+1 < length){
                int start = index.value;
                // 如果是小数点开头的数字，完整读取整个数字并报错
                lexeme.append(c);
                index.value++;
                while (index.value < length && Character.isDigit(input.charAt(index.value))) {
                    lexeme.append(input.charAt(index.value++));
                }
                errorHandler.addError(currentLine, currentPos,"Invalid numeric format: " + lexeme.toString());
                tokens.add(new Token(TokenType.ERROR, lexeme.toString(), currentLine, start));
                lexeme.setLength(0);
                currentPos += index.value - start;
                continue;
            }
            // 处理标识符和关键字
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

            // 处理字符串常量
            if (c == '"') {
                int start = index.value++;
                while (index.value < length && input.charAt(index.value) != '"') {
                    lexeme.append(input.charAt(index.value++));
                }
                if (index.value >= length) {
                    errorHandler.addError(currentLine, currentPos,"Unclosed string literal");
                } else {
                    index.value++;
                    tokens.add(new Token(TokenType.STRING_CONST, lexeme.toString(), currentLine, start));
                    lexeme.setLength(0);
                }
                currentPos += index.value - start;
                continue;
            }

            // 处理字符常量
            if (c == '\'') {
                int startLine = currentLine;
                int startPos = currentPos;
                int startIndex = index.value;
                index.value++; // 跳过开始的单引号

                if (index.value >= input.length()) {
                    errorHandler.addError(currentLine, currentPos,"Unclosed character literal");
                    tokens.add(new Token(TokenType.ERROR, "'", currentLine, startPos));
                    index.value++;
                    currentPos++;
                    continue;
                }

                char charValue = input.charAt(index.value);
                boolean isEscape = false;

                // 处理转义字符
                if (charValue == '\\') {
                    isEscape = true;
                    index.value++;
                    if (index.value >= input.length()) {
                        errorHandler.addError(currentLine, currentPos,"Incomplete escape sequence");
                        tokens.add(new Token(TokenType.ERROR, "'\\", currentLine, startPos));
                        currentPos += 2;
                        index.value++;
                        continue;
                    }
                    char escapeChar = input.charAt(index.value);
                    switch (escapeChar) {
                        case 'n': charValue = '\n'; break;
                        case 't': charValue = '\t'; break;
                        case 'r': charValue = '\r'; break;
                        case 'b': charValue = '\b'; break;
                        case 'f': charValue = '\f'; break;
                        case '\'': charValue = '\''; break;
                        case '\\': charValue = '\\'; break;
                        default:
                            errorHandler.addError(currentLine, currentPos,"Invalid escape sequence: \\" + escapeChar);
                            tokens.add(new Token(TokenType.ERROR, "'\\" + escapeChar, currentLine, startPos));
                            index.value++;
                            currentPos += 3;
                            continue;
                    }
                    index.value++;
                }

                index.value++; // 跳过字符或转义后的字符

                // 检查闭合的单引号
                if (index.value >= input.length() || input.charAt(index.value) != '\'') {
                    errorHandler.addError(currentLine, currentPos,"Unclosed character literal");
                    tokens.add(new Token(TokenType.ERROR, "'" + charValue, currentLine, startPos));
                    if (index.value < input.length()) index.value++;
                    currentPos += (index.value - startIndex);
                } else {
                    index.value++; // 跳过闭合的单引号
                    tokens.add(new Token(TokenType.CHAR_CONST, String.valueOf(charValue), currentLine, startPos));
                    currentPos += (index.value - startIndex);
                }
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
                errorHandler.addError(currentLine, currentPos,"Illegal character: " + c);
                tokens.add(new Token(TokenType.ERROR, Character.toString(c), currentLine, index.value));
                index.value++;
                currentPos++;
            }
        }
        return tokens;
    }
    // 综合预处理入口
    public  String preprocess(String source) {
        String withoutComments = removeComments(source);
        return removeWhitespace(withoutComments);
    }

    // 删除所有类型的注释
    private static String removeComments(String input) {
        // 分两步处理不同注释类型
        String noMultiLine = removeMultiLineComments(input);
        return removeSingleLineComments(noMultiLine);
    }

    // 删除多行注释 /* ... */
    private static String removeMultiLineComments(String input) {
        Pattern pattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll("");
    }

    // 删除单行注释 //
    private static String removeSingleLineComments(String input) {
        Pattern pattern = Pattern.compile("//.*");
        Matcher matcher = pattern.matcher(input);
        return matcher.replaceAll("");
    }

    // 删除空白符（保留字符串内容）
    private static String removeWhitespace(String input) {
        StringBuilder output = new StringBuilder();
        boolean inString = false;
        boolean lastCharIsSlash = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // 处理字符串状态切换（考虑转义字符）
            if (c == '"' && !lastCharIsSlash) {
                inString = !inString;
            }

            // 记录是否为转义斜杠
            lastCharIsSlash = (c == '\\' && !lastCharIsSlash);

            // 在字符串内保留所有字符
            if (inString) {
                output.append(c);
                continue;
            }

            // 删除字符串外的空白符
            if (!Character.isWhitespace(c)) {
                output.append(c);
            }
        }
        return output.toString();
    }
    // 预处理逻辑（移除注释、统一大小写等）
    String preprocessInput(String input) {
        return input.replaceAll("//.*", "")  // 移除单行注释
                .replaceAll("/\\*.*?\\*/", ""); // 移除多行注释
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

            errorHandler.addError(currentLine,currentPos, buffer.toString());
            tokens.add(new Token(TokenType.ERROR, buffer.toString(), startLine, startPos));
            return;
        }

        // 正常数值常量处理
        tokens.add(new Token(TokenType.NUMERIC_CONST, buffer.toString(), startLine, startPos));
    }

    private boolean isHexDigit(char c) {
        return Character.digit(c, 16) != -1;
    }
    private boolean isOctalDigit(char c) {
        return c >= '0' && c <= '7';
    }
    public String getTokenList() {
        StringBuilder sb = new StringBuilder();
        tokens.forEach(t -> sb.append(t).append("\n"));
        return sb.toString();
    }
    public String getSymbolTable() {
        return symbolTable.toString();
    }

    public String getErrorList() {
        return errorHandler.toString();
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
    class Error {
        int line;
        int pos;
        String message;
        public Error(int line, int pos, String message) {
            this.line = line;
            this.pos = pos;
            this.message = message;
        }
    }
    private final List<Error> errors = new ArrayList<>();
//    private final Map<Integer, List<String>> errors = new HashMap<>();

    public void addError(int line, int pos,String message) {
        errors.add(new Error(line, pos, message));
//        errors.computeIfAbsent(line, k -> new ArrayList<>()).add(message);
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        errors.forEach((e) -> {
            sb.append("Line ").append(e.line).append(", Position ").append(e.pos).append(": ").append(e.message).append("\n");
//            messages.forEach(msg -> sb.append("Line ").append(line).append(": ").append(msg).append("\n"));
        });
        return sb.toString();
    }
}