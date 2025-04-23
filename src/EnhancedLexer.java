import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum TokenType {
    KEYWORD, IDENTIFIER, NUMERIC_CONST, STRING_CONST, CHAR_CONST,
    OPERATOR, DELIMITER, ERROR
}

class Token {
    private static final int POSITION_OFFSET = 1;
    public final TokenType type;
    public final String value;
    public final int line;
    public final int position;

    public Token(TokenType type, String value, int line, int position) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.position = position + POSITION_OFFSET; // 列号从1开始
    }

    @Override
    public String toString() {
        return String.format("<%s, %s>",
                type.name(), value );
    }
}

class EnhancedLexer {
    // 常量定义
    private static final int DEFAULT_STRING_BUILDER_CAPACITY = 128;
    private static final int MAX_ESCAPE_SEQUENCE_LENGTH = 4;
    private static final char EOF = (char)-1;

    // 预编译正则表达式
    private static final Pattern HEX_PATTERN = Pattern.compile("^0[xX][0-9a-fA-F]+$");
    private static final Pattern OCTAL_PATTERN = Pattern.compile("^0[0-7]+$");
    private static final Pattern SCIENTIFIC_PATTERN =
            Pattern.compile("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$");
    final Pattern DECIMAL_PATTERN = Pattern.compile("^\\d+(\\.\\d*)?$");
    // 状态枚举
    private enum ParseResult { CONTINUE, COMPLETE, ERROR }

    private int currentLine = 1;
    private int currentColumn = 1;
    private final SymbolTable symbolTable = new SymbolTable();
    private final ErrorHandler errorHandler = new ErrorHandler();
    private final List<Token> tokens = new ArrayList<>();
    private NumberState currentNumberState = NumberState.INITIAL;//数值解析状态跟踪

    // 关键字集合（使用Set加速查找）
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "where", "if", "else", "break", "continue",
            "int", "double", "float", "char", "unsigned",
            "return", "goto", "then", "static", "new",
            "case", "switch", "default", "true", "false",
            "bool", "for", "while", "do", "sizeof", "typedef",
            "struct", "union", "enum", "const", "volatile",
            "register", "extern", "auto", "void", "short",
            "long", "signed", "unsigned", "sizeof"
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
        String processed = preprocess(input);
        processInput(processed);
        return Collections.unmodifiableList(tokens);
    }
    // 预处理函数
    public String preprocess(String input) {
        String removedComments = removeComments(input);
//        return removeWhitespace(removedComments);
        return removedComments;
//        return compactWhitespace(removedComments);
    }
    public String pre(String input) {
        String removedComments = removeComments(input);
        return removeWhitespace(removedComments);
//        return removedComments;
    }

    //词法分析核心模块
    private void processInput(String input) {
        int length = input.length();
        int index = 0;

        while (index < length) {
            char current = input.charAt(index);
            int startPos = currentColumn;
            // 处理空格：跳过并更新位置
            if (Character.isWhitespace(current)) {
                if (current == '\n' || current == '\r') {
                    currentLine++;
                    currentColumn = 1;
                } else {
                    currentColumn++;
                }
                index++;
                continue;
            }else if (Character.isDigit(current)||current=='.') {
                index = processNumber(input, index);
            } else if (current == '\'') {
                index = processCharLiteral(input, index);
            } else if (current == '"') {
                index = processStringLiteral(input, index);
            } else if (OPERATORS.containsKey(Character.toString(current))) {
                index = processOperator(input, index);
            } else if (DELIMITERS.contains(current)) {
                tokens.add(createToken(TokenType.DELIMITER, Character.toString(current)));
                index++;
            } else if (Character.isLetter(current) || current == '_') {
                index = processIdentifier(input, index);
            }else {
                handleInvalidCharacter(current, index);
                index++;
            }
            updatePosition(input, index);
            index++;
        }
    }

    // 数字处理
    private int processNumber(String input, int start) {
        NumberState state = NumberState.INITIAL;
        StringBuilder buffer = new StringBuilder(DEFAULT_STRING_BUILDER_CAPACITY);
        int index = start;

        while (index < input.length()) {
            char c = (index < input.length()) ? input.charAt(index) : EOF;
            ParseResult result = handleNumberState(c, state, buffer);

            if (result == ParseResult.COMPLETE) break;
            if(result==ParseResult.CONTINUE)
                if (result == ParseResult.ERROR) {
                    handleNumberError(buffer.toString(), start);
                    return index + 1;
                }

            state = transitionNumberState(c, state);

            index++;
        }

        validateNumber(buffer.toString(), start);
        return index;
    }

    //处理转义序列
    private int processEscapeSequence(String input, int index, StringBuilder buffer) {
        final int MAX_OCTAL_LENGTH = 3;
        final int MAX_UNICODE_LENGTH = 4;

        if (index + 1 >= input.length()) {
            errorHandler.addError(currentLine, currentColumn, "未结束的转义序列");
            buffer.append('\\');
            return index;
        }

        char escapeChar = input.charAt(index + 1);
        switch (escapeChar) {
            // 标准转义字符
            case 'n': buffer.append('\n'); break;
            case 't': buffer.append('\t'); break;
            case 'r': buffer.append('\r'); break;
            case 'b': buffer.append('\b'); break;
            case 'f': buffer.append('\f'); break;
            case '\'': buffer.append('\''); break;
            case '"': buffer.append('"'); break;
            case '\\': buffer.append('\\'); break;

            // Unicode转义
            case 'u':
                if (index + 5 >= input.length()) {
                    errorHandler.addError(currentLine, currentColumn, "不完整的Unicode转义");
                    return index + 1;
                }
                String hex = "";
                try {
                    hex = input.substring(index + 2, index + 6);
                    buffer.append((char) Integer.parseInt(hex, 16));
                    return index + 5;
                } catch (NumberFormatException e) {
                    errorHandler.addError(currentLine, currentColumn, "非法的Unicode字符: \\u" + hex);
                    return index + 5;
                }

                // 八进制转义
            case '0': case '1': case '2': case '3':
            case '4': case '5': case '6': case '7':
                int end = Math.min(index + 1 + MAX_OCTAL_LENGTH, input.length());
                String octalDigits = input.substring(index + 1, end);
                Matcher m = Pattern.compile("^[0-7]{1,3}").matcher(octalDigits);
                if (m.find()) {
                    try {
                        int code = Integer.parseInt(m.group(), 8);
                        if (code > 0xFF) {
                            errorHandler.addError(currentLine, currentColumn,
                                    "八进制值超出范围: \\" + m.group());
                        }
                        buffer.append((char) code);
                        return index + m.group().length();
                    } catch (NumberFormatException e) {
                        // 不会发生
                    }
                }
                break;
            //十六进制
            case 'x': // 十六进制转义处理
                int hexStart = index + 2; // 跳过反斜杠和x
                int hexEnd = Math.min(hexStart + 2, input.length()); // 十六进制最多2位
                String hexDigits = input.substring(hexStart, hexEnd);

                // 验证十六进制格式
                Matcher hexMatcher = Pattern.compile("^[0-9a-fA-F]{1,2}$").matcher(hexDigits);
                if (hexMatcher.find()) {
                    try {
                        int code = Integer.parseInt(hexDigits, 16); // 基数改为16
                        if (code > 0xFF) {
                            errorHandler.addError(currentLine, currentColumn,
                                    "十六进制值超出范围: \\x" + hexDigits);
                        }
                        buffer.append((char) code);
                        return hexEnd - 1; // 正确跳转到处理后的位置
                    } catch (NumberFormatException e) {
                        // 不会发生
                    }
                } else {
                    errorHandler.addError(currentLine, currentColumn,
                            "无效的十六进制转义: \\x" + hexDigits);
                }
                break;
            default:
                errorHandler.addError(currentLine, currentColumn,
                        "无效的转义字符: \\" + escapeChar);
                buffer.append('\\').append(escapeChar);
        }

        return index + 2;
    }

    //数值解析状态处理核心逻辑
    private ParseResult handleNumberState(char c, NumberState state, StringBuilder buffer) {
        // 结束条件检查
        if (!(
                Character.isDigit(c) ||
                        c == '.' ||
                        (state == NumberState.ZERO_PREFIX && (c == 'x' || c == 'X')) ||  // 新增ZERO_PREFIX状态判断
                        (state == NumberState.HEX_PREFIX && Character.digit(c, 16) != -1) ||
                        (state == NumberState.HEX && Character.digit(c, 16) != -1) ||
                        (state == NumberState.OCTAL && c >= '0' && c <= '7') ||
                        (state == NumberState.DECIMAL && Character.isDigit(c)) ||
                        (state == NumberState.EXPONENT && (Character.isDigit(c) || c == '+' || c == '-')) ||
                        (state == NumberState.EXP_SIGN && Character.isDigit(c)) ||
                        (state == NumberState.EXP_DIGIT && Character.isDigit(c)) ||
                        (state == NumberState.INITIAL && c == '0') ||
                        ((state == NumberState.INITIAL || state == NumberState.DECIMAL) && (c == 'e' || c == 'E'))

        )) {
            return ParseResult.COMPLETE;
        }

        // 状态转换逻辑
        switch (state) {
            case INITIAL:
                if (c == '0') {
                    buffer.append(c);
                    return transition(NumberState.ZERO_PREFIX);
                }
                if (Character.isDigit(c)) {
                    buffer.append(c);
                    return continueParsing();
                }
                if (c == 'e' || c == 'E') {
                    buffer.append(c);
                    return transition(NumberState.EXPONENT);
                }

                break;

            case ZERO_PREFIX:
                if (c == 'x' || c == 'X') {
                    buffer.append(c);
                    return transition(NumberState.HEX_PREFIX);
                }
                if (c >= '0' && c <= '7') {
                    buffer.append(c);
                    return transition(NumberState.OCTAL);
                }
                if (c == '.') {
                    buffer.append(c);
                    return transition(NumberState.DECIMAL);
                }
                break;

            case HEX_PREFIX:
                if (Character.digit(c, 16) != -1) {
                    buffer.append(c);
                    return transition(NumberState.HEX);
                }
                reportError("十六进制需要数字", buffer.length());
                return ParseResult.ERROR;

            case HEX:
                if (Character.digit(c, 16) != -1) {
                    buffer.append(c);
                    return continueParsing();
                }
                reportError("十六进制需要数字", buffer.length());
                return ParseResult.ERROR;

            case OCTAL:
                if (c >= '0' && c <= '7') {
                    buffer.append(c);
                    return continueParsing();
                }
                reportError("八进制需要数字", buffer.length());
                break;
            case DECIMAL:
                if (Character.isDigit(c)) {
                    buffer.append(c);
                    return continueParsing();
                }
                if (c == 'e' || c == 'E') {
                    buffer.append(c);
                    return transition(NumberState.EXPONENT);
                }
                if (c == '.') {
                    if(buffer.indexOf(".") != -1){
                        reportError("小数点只能出现一次", buffer.length());
                        return ParseResult.ERROR;
                    }
                    buffer.append(c);
                    return transition(NumberState.DECIMAL);
                }

                break;

            case EXPONENT:
                if (c == '+' || c == '-') {
                    buffer.append(c);
                    return transition(NumberState.EXP_SIGN);
                }
                if (Character.isDigit(c)) {
                    buffer.append(c);
                    return transition(NumberState.EXP_DIGIT);
                }
                reportError("指数需要数字", buffer.length());
                return ParseResult.ERROR;

            case EXP_SIGN:
                if (Character.isDigit(c)) {
                    buffer.append(c);
                    return transition(NumberState.EXP_DIGIT);
                }
                reportError("符号后需要指数", buffer.length());
                return ParseResult.ERROR;

            case EXP_DIGIT:
                if (Character.isDigit(c)) {
                    buffer.append(c);
                    return continueParsing();
                }
                break;
        }

        return ParseResult.COMPLETE;
    }

    // 辅助方法
    // 更新当前状态机状态
    private ParseResult transition(NumberState newState) {
        currentNumberState = newState;
        return ParseResult.CONTINUE;
    }

    private ParseResult continueParsing() {
        return ParseResult.CONTINUE;
    }

    // 字符常量处理
    private int processCharLiteral(String input, int start) {
        StringBuilder buffer = new StringBuilder(MAX_ESCAPE_SEQUENCE_LENGTH);
        int index = start + 1; // 跳过开始的'
        boolean valid = true;

        while (index < input.length()) {
            char c = input.charAt(index);
            if (c == '\'') {
                if (buffer.length() == 0) {
                    reportError("Empty character literal", currentColumn);
                    return index + 1;
                }else if(buffer.length() > 1) {
                    reportError("Character literal too long", currentColumn);
                    return index + 1;
                }
                tokens.add(createToken(TokenType.CHAR_CONST, buffer.toString()));
                return index + 1;
            }else if (c == '\\') {
                index = processEscapeSequence(input, index, buffer);
                if (index == -1) valid = false;
                continue;
//                else tokens.add(createToken(TokenType.CHAR_CONST, buffer.toString()));
            } else {
                buffer.append(c);
            }
            index++;
        }

//        if (valid) reportError("Unclosed character literal", start);
        return index;
    }

    // 字符串常量处理
    private int processStringLiteral(String input, int startIndex) {
        StringBuilder buffer = new StringBuilder();
        int startLine = currentLine;
        int startColumn = currentColumn;
        int index = startIndex + 1; // 跳过起始引号
        boolean escapeMode = false;
        boolean valid = true;

        while (index < input.length()) {
            char c = input.charAt(index);

            if (c == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }

            if (escapeMode) {
                switch (c) {
                    case 'n': buffer.append('\n'); break;
                    case 't': buffer.append('\t'); break;
                    case 'r': buffer.append('\r'); break;
                    case '"': buffer.append('"'); break;
                    case '\\': buffer.append('\\'); break;
                    default:
                        reportError( "无效的转义序列: \\" + c, currentColumn);
//                        errorHandler.addError(currentLine, "无效的转义序列: \\" + c);
                        valid = false;
                }
                escapeMode = false;
            } else if (c == '\\') {
                escapeMode = true;
            } else if (c == '"') {
                if (valid) {
                    tokens.add(new Token(TokenType.STRING_CONST, buffer.toString(),
                            startLine, startColumn));
                } else {
                    tokens.add(new Token(TokenType.ERROR, buffer.toString(),
                            startLine, startColumn));
                }
                return index + 1;
            } else {
                buffer.append(c);
            }
            index++;
        }
        reportError("未闭合的字符串常量", startIndex);
//        errorHandler.addError(startLine, "未闭合的字符串常量");
        tokens.add(new Token(TokenType.ERROR, buffer.toString(),
                startLine, startColumn));
        return index;
    }

    // 运算符处理（支持多字符运算符）
    private int processOperator(String input, int startIndex) {
        int endIndex = startIndex + 1;
        String maxOp = input.substring(startIndex, Math.min(endIndex+1, input.length()));

        // 查找最长匹配运算符
        while (endIndex <= input.length()) {
            String currentOp = input.substring(startIndex, endIndex);
            if (OPERATORS.keySet().stream().noneMatch(op -> op.startsWith(currentOp))) {
                break;
            }
            if (OPERATORS.containsKey(currentOp)) {
                maxOp = currentOp;
            }
            endIndex++;
        }

        tokens.add(new Token(TokenType.OPERATOR, maxOp,
                currentLine, currentColumn));
        currentColumn += maxOp.length();
        return startIndex + maxOp.length();
    }

    // 标识符处理（带符号表管理）
    private int processIdentifier(String input, int startIndex) {
        int endIndex = startIndex + 1;
        while (endIndex < input.length()) {
            char c = input.charAt(endIndex);
            if (!Character.isLetterOrDigit(c) && c != '_') break;
            endIndex++;
        }

        String identifier = input.substring(startIndex, endIndex);
        TokenType type = KEYWORDS.contains(identifier) ?
                TokenType.KEYWORD : TokenType.IDENTIFIER;

        tokens.add(new Token(type, identifier, currentLine, currentColumn));
        if (type == TokenType.IDENTIFIER) {
            symbolTable.addIdentifier(identifier);
        }

        currentColumn += endIndex - startIndex;
        return endIndex;
    }

    // 错误字符处理
    private void handleInvalidCharacter(char c, int index) {
        String errorMsg = String.format("非法字符: 0x%02x '%c'", (int)c, c);
        reportError(errorMsg, currentColumn);
//        errorHandler.addError(currentLine, errorMsg);
        tokens.add(new Token(TokenType.ERROR, Character.toString(c),
                currentLine, currentColumn));
        currentColumn++;
    }

    // 辅助方法
    private Token createToken(TokenType type, String value) {
        return new Token(type, value, currentLine, currentColumn);
    }

    private void reportError(String message, int position) {
        errorHandler.addError(currentLine, position, message);
        tokens.add(createToken(TokenType.ERROR, message));
    }

    private void validateNumber(String value, int startPos) {
        Number number = null;
        try {
            // 1. 十六进制验证
            if (HEX_PATTERN.matcher(value).matches()) {
                number = Long.parseLong(value.substring(2), 16);
            }
            // 2. 八进制验证
            else if (OCTAL_PATTERN.matcher(value).matches()) {
                // 禁止08这样的形式
                if (value.length() > 1 && value.charAt(1) == '8') {
                    reportError("Invalid octal digit '8'", startPos + 1);
                }
                number = Long.parseLong(value, 8);
            }
            // 4. 普通十进制验证
            else if (DECIMAL_PATTERN.matcher(value).matches()) {
                // 检查前导零
                if (value.length() > 1 && value.startsWith("0") &&
                        !value.contains(".")) {
                    reportError("Invalid decimal digit '0'", startPos + 1);
                }
                number = new BigDecimal(value);
            }
            // 3. 科学计数法验证
            else if (SCIENTIFIC_PATTERN.matcher(value).matches()) {
                // 分解指数部分验证
                String[] parts = value.split("[eE]");
                if (parts.length > 1) {
                    String exponent = parts[1];
                    if (exponent.isEmpty() ||
                            !exponent.matches("^[+-]?\\d+$")) {
                        reportError("Invalid exponent format", startPos + value.indexOf('e'));

                    }
                }
                number = Double.parseDouble(value);
            }

            // 5. 无效格式
            else {
                reportError("Malformed number", startPos);
                return;
            }

            // 验证通过则添加合法token
            tokens.add(new Token(TokenType.NUMERIC_CONST, number.toString(), currentLine, startPos));

        } catch (NumberFormatException e) {
            // 生成精准错误信息
            String errorMsg = "Invalid numeric format '" + value + "': " + e.getMessage();
            reportError(errorMsg, startPos);
            tokens.add(new Token(TokenType.ERROR, value, currentLine, startPos));
        }
    }

    // 数值解析状态枚举
    private enum NumberState {
        INITIAL,        // 初始状态
        ZERO_PREFIX,    // 前导零
        DECIMAL,        // 小数部分
        HEX_PREFIX,     // 十六进制前缀(x/X)
        HEX,            // 十六进制数字
        OCTAL,          // 八进制数字
        EXPONENT,       // 指数符号(e/E)
        EXP_SIGN,       // 指数符号后的正负号
        ERROR, EXP_DIGIT       // 指数数字
    }

    // 错误处理函数
    private void handleNumberError(String invalidNumber, int startPosition) {
        String errorType = "数值格式错误";

        // 错误类型判断
        if (invalidNumber.contains(".")) {
            if (invalidNumber.indexOf('.') != invalidNumber.lastIndexOf('.')) {
                errorType = "多个小数点";
            } else if (invalidNumber.endsWith(".")) {
                errorType = "不完整的小数";
            }
        } else if (invalidNumber.matches("0[xX].*")) {
            if (invalidNumber.length() == 2) {
                errorType = "十六进制数缺少数字";
            } else if (!invalidNumber.substring(2).matches("[0-9a-fA-F]+")) {
                errorType = "非法十六进制字符";
            }
        } else if (invalidNumber.matches("0[0-7]*[89].*")) {
            errorType = "八进制包含非法数字";
        } else if (invalidNumber.matches(".*[eE].*")) {
            if (!invalidNumber.matches(".*[eE][+-]?\\d+")) {
                errorType = "指数部分不完整";
            }
        }

//        // 记录错误
//        errorHandler.addError(currentLine, startPosition,
//                String.format("%s: %s", errorType, invalidNumber));

//        // 生成错误token
//        tokens.add(new Token(TokenType.ERROR, invalidNumber,
//                currentLine, startPosition));
    }

    // 状态转换函数
    private NumberState transitionNumberState(char currentChar, NumberState currentState) {
        switch (currentState) {
            case INITIAL:
                if (currentChar == '0') return NumberState.ZERO_PREFIX;
                if (Character.isDigit(currentChar)) return NumberState.DECIMAL;
//                if (currentChar == 'e' || currentChar == 'E') return NumberState.EXPONENT;
//                if (currentChar == '.') return NumberState.DECIMAL;
                break;
            case ZERO_PREFIX:
                if (currentChar == 'x' || currentChar == 'X') return NumberState.HEX_PREFIX;
                if (currentChar == '.') return NumberState.DECIMAL;
                if (currentChar >= '0' && currentChar <= '7') return NumberState.OCTAL;
                if (currentChar == '8' || currentChar == '9') return NumberState.ERROR;
                break;

            case HEX_PREFIX:
                if (isHexDigit(currentChar)) return NumberState.HEX;
                break;

            case HEX:
                if (isHexDigit(currentChar)) return NumberState.HEX;
                if (currentChar == '.') return NumberState.ERROR; // 十六进制不支持小数
                break;

            case OCTAL:
                if (currentChar >= '0' && currentChar <= '7') return NumberState.OCTAL;
                if (currentChar == '8' || currentChar == '9') return NumberState.ERROR;

                break;

            case DECIMAL:
                if (Character.isDigit(currentChar)) return NumberState.DECIMAL;
                if (currentChar == '.') return NumberState.DECIMAL;
                if (currentChar == 'e' || currentChar == 'E')
                    return NumberState.EXPONENT;
                break;

            case EXPONENT:
                if (currentChar == '+' || currentChar == '-') return NumberState.EXP_SIGN;
                if (Character.isDigit(currentChar)) return NumberState.EXP_DIGIT;
                break;

            case EXP_SIGN:
            case EXP_DIGIT:
                if (Character.isDigit(currentChar)) return NumberState.EXP_DIGIT;
                break;
        }
        return NumberState.ERROR; // 无法识别的转换
    }

    // 辅助方法：十六进制字符检查
    private boolean isHexDigit(char c) {
        return Character.digit(c, 16) >= 0;
    }

    // 位置跟踪
    private void updatePosition(String input, int index) {
        if (index >= input.length()) return;
        if (input.charAt(index) == '\n' || input.charAt(index) == '\r') {
            currentLine++;
            currentColumn = 1;
        } else {
            currentColumn++;
        }
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
    // 预处理优化
    private static String compactWhitespace(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        boolean inWhitespace = false;
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                if (!inWhitespace) sb.append(' ');
                inWhitespace = true;
            } else {
                sb.append(c);
                inWhitespace = false;
            }
        }
        return sb.toString();
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
}

class SymbolTable {
    private final Map<String, Integer> identifiers = new LinkedHashMap<>();
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
    static class Error {
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

    public void addError(int line, int pos,String message) {
        errors.add(new Error(line, pos, message));
    }

    public boolean isEmpty() {
        return errors.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        errors.forEach((e) -> {
            sb.append("Line ").append(e.line).append(", Position ").append(e.pos).append(": ").append(e.message).append("\n");
        });
        return sb.toString();
    }
}