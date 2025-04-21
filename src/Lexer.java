///**
// * @Author: hahaha
// * @Package:PACKAGE_NAME
// * @Project:word_translator
// * @name:Le
// * @Date:2025/4/21
// * @Time:18:37
// * @Filename:Le
// **/
//import java.util.*;
//import java.util.regex.Pattern;
//
///**
// * 词法分析器优化版
// * 主要改进：
// * 1. 使用状态模式优化状态转移
// * 2. 增强符号表管理
// * 3. 改进错误处理机制
// * 4. 优化正则表达式性能
// * 5. 增加代码可测试性
// */
//// 词法单元类型枚举
//enum TokenType {
//    KEYWORD, IDENTIFIER, NUMERIC_CONST, STRING_CONST,
//    CHAR_CONST, OPERATOR, DELIMITER, ERROR
//}
//
//// 词法单元类
//class Lexeme {
//    private static final String TYPE_FORMAT = "%-12s";
//    private static final String POS_FORMAT = "%-8s";
//
//    final String value;
//    final TokenType type;
//    final int line;
//    final int column;
//    final int position;
//
//    public Lexeme(String value, TokenType type, int line, int column, int pos) {
//        this.value = value;
//        this.type = type;
//        this.line = line;
//        this.column = column;
//        this.position = pos;
//    }
//
//    public String toFormattedString() {
//        return String.format(TYPE_FORMAT + POS_FORMAT + "%-4d%-4d%s",
//                type.toString().replace('_', ' ') + ",",
//                "(" + position + "),",
//                line,
//                column,
//                value);
//    }
//}
//
//// 符号表条目
//class SymbolEntry {
//    final String name;
//    final TokenType type;
//    final int declarationLine;
//    final int scopeLevel;
//    Object value;
//
//    public SymbolEntry(String name, TokenType type, int line, int scope) {
//        this.name = name;
//        this.type = type;
//        this.declarationLine = line;
//        this.scopeLevel = scope;
//    }
//}
//
//// 符号表管理
//class SymbolTable {
//    private final Map<String, SymbolEntry> table = new LinkedHashMap<>();
//    private int currentScope = 0;
//
//    public void enterScope() { currentScope++; }
//    public void exitScope() { currentScope--; }
//
//    public boolean addSymbol(String name, TokenType type, int line) {
//        if (table.containsKey(name) && table.get(name).scopeLevel == currentScope) {
//            return false;
//        }
//        table.put(name, new SymbolEntry(name, type, line, currentScope));
//        return true;
//    }
//
//    public String getFormattedTable() {
//        StringBuilder sb = new StringBuilder();
//        sb.append(String.format("%-20s%-12s%-8s%-8s\n",
//                "Name", "Type", "Line", "Scope"));
//        table.forEach((k,v) -> sb.append(String.format("%-20s%-12s%-8d%-8d\n",
//                k,
//                v.type.toString().replace('_', ' '),
//                v.declarationLine,
//                v.scopeLevel)));
//        return sb.toString();
//    }
//}
//
//// 错误处理增强
//class ErrorCollector {
//    private final List<Lexeme> errors = new ArrayList<>();
//
//    public void addError(String value, int line, int col, int pos, String msg) {
//        errors.add(new Lexeme(String.format("%s (原因: %s)", value, msg),
//                TokenType.ERROR, line, col, pos));
//    }
//
//    public String getErrorReport() {
//        if (errors.isEmpty()) return "未发现语法错误\n";
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("错误列表:\n");
//        errors.forEach(e -> sb.append(e.toFormattedString()).append("\n"));
//        return sb.toString();
//    }
//}
//
//// 词法分析核心
//public class Lexer {
//    private static final Pattern IDENTIFIER_PATTERN =
//            Pattern.compile("^[a-zA-Z_]\\w*$");
//    private static final Pattern NUMERIC_PATTERN =
//            Pattern.compile("^(?:\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?" +
//                    "|0[xX][\\da-fA-F]+" +
//                    "|0[bB][01]+)$");
//
//    private final SymbolTable symbolTable = new SymbolTable();
//    private final ErrorCollector errorCollector = new ErrorCollector();
//    private final List<Lexeme> tokens = new ArrayList<>();
//    private final StringBuilder output = new StringBuilder();
//
//    private enum ProcessState {
//        INIT, IDENT, NUMBER, OPERATOR,
//        STRING, CHAR, LINE_COMMENT,
//        BLOCK_COMMENT, ERROR
//    }
//
//    public String analyze(String source) {
//        ProcessState state = ProcessState.INIT;
//        StringBuilder buffer = new StringBuilder(32);
//        int line = 1, column = 1, pos = 0;
//        char[] chars = source.toCharArray();
//
//        for (int i = 0; i < chars.length; i++) {
//            char c = chars[i];
//            pos++;
//            column++;
//
//            if (c == '\n') {
//                line++;
//                column = 1;
//            }
//
//            switch (state) {
//                case INIT:
//                    handleInitialState(c, buffer, line, column, pos, i, chars);
//                    break;
//
//                case IDENT:
//                    if (Character.isLetterOrDigit(c) || c == '_') {
//                        buffer.append(c);
//                    } else {
//                        processIdentifier(buffer, line, column, pos);
//                        i--; // 回退处理当前字符
//                        state = ProcessState.INIT;
//                    }
//                    break;
//
//                case NUMBER:
//                    if (Character.isDigit(c) || c == '.' ||
//                            c == 'x' || c == 'X' || c == 'b' || c == 'B' ||
//                            (buffer.length() > 0 &&
//                                    (buffer.indexOf("e") != -1 || buffer.indexOf("E") != -1) &&
//                                    (c == '+' || c == '-'))) {
//                        buffer.append(c);
//                    } else {
//                        processNumber(buffer, line, column, pos);
//                        i--;
//                        state = ProcessState.INIT;
//                    }
//                    break;
//
//                case OPERATOR:
//                    if (WordList.OPERATORS.contains(buffer.toString() + c)) {
//                        buffer.append(c);
//                    } else {
//                        processOperator(buffer, line, column, pos);
//                        i--;
//                        state = ProcessState.INIT;
//                    }
//                    break;
//
//                case STRING:
//                    processString(c, buffer, line, column, pos, i, chars);
//                    if (c == '"') state = ProcessState.INIT;
//                    break;
//
//                case CHAR:
//                    processChar(c, buffer, line, column, pos, i, chars);
//                    if (c == '\'') state = ProcessState.INIT;
//                    break;
//
//                case LINE_COMMENT:
//                    if (c == '\n') state = ProcessState.INIT;
//                    break;
//
//                case BLOCK_COMMENT:
//                    if (c == '*' && i+1 < chars.length && chars[i+1] == '/') {
//                        i++;
//                        state = ProcessState.INIT;
//                    }
//                    break;
//            }
//        }
//
//        // 处理未结束的token
//        processUnclosedTokens(buffer, state, line, column, pos);
//
//        // 生成最终输出
//        generateOutput();
//        return output.toString();
//    }
//
//    private void handleInitialState(char c, StringBuilder buffer,
//                                    int line, int column, int pos,
//                                    int index, char[] chars) {
//        if (Character.isWhitespace(c)) return;
//
//        buffer.setLength(0);
//        buffer.append(c);
//
//        if (Character.isLetter(c) || c == '_') {
//            ProcessState.IDENT.name();
//        } else if (Character.isDigit(c)) {
//            ProcessState.NUMBER.name();
//        } else if (WordList.OPERATORS.contains(String.valueOf(c))) {
//            ProcessState.OPERATOR.name();
//        } else if (c == '"') {
//            ProcessState.STRING.name();
//        } else if (c == '\'') {
//            ProcessState.CHAR.name();
//        } else if (c == '/' && index+1 < chars.length) {
//            if (chars[index+1] == '/') {
//                ProcessState.LINE_COMMENT.name();
//                index++;
//            } else if (chars[index+1] == '*') {
//                ProcessState.BLOCK_COMMENT.name();
//                index++;
//            }
//        } else if (WordList.DELIMITERS.contains(String.valueOf(c))) {
//            tokens.add(new Lexeme(String.valueOf(c), TokenType.DELIMITER,
//                    line, column, pos));
//        } else {
//            errorCollector.addError(String.valueOf(c), line, column, pos,
//                    "非法字符");
//        }
//    }
//
//    private void processIdentifier(StringBuilder buffer, int line,
//                                   int column, int pos) {
//        String ident = buffer.toString();
//        if (WordList.KEYWORDS.contains(ident)) {
//            tokens.add(new Lexeme(ident, TokenType.KEYWORD, line, column, pos));
//        } else if (IDENTIFIER_PATTERN.matcher(ident).matches()) {
//            tokens.add(new Lexeme(ident, TokenType.IDENTIFIER, line, column, pos));
//            symbolTable.addSymbol(ident, TokenType.IDENTIFIER, line);
//        } else {
//            errorCollector.addError(ident, line, column, pos,
//                    "非法标识符格式");
//        }
//        buffer.setLength(0);
//    }
//
//    private void processNumber(StringBuilder buffer, int line,
//                               int column, int pos) {
//        String num = buffer.toString();
//        if (NUMERIC_PATTERN.matcher(num).matches()) {
//            tokens.add(new Lexeme(num, TokenType.NUMERIC_CONST, line, column, pos));
//        } else {
//            errorCollector.addError(num, line, column, pos,
//                    "非法数字格式");
//        }
//        buffer.setLength(0);
//    }
//
//    private void processOperator(StringBuilder buffer, int line,
//                                 int column, int pos) {
//        String op = buffer.toString();
//        if (WordList.OPERATORS.contains(op)) {
//            tokens.add(new Lexeme(op, TokenType.OPERATOR, line, column, pos));
//        } else {
//            errorCollector.addError(op, line, column, pos,
//                    "未定义的操作符");
//        }
//        buffer.setLength(0);
//    }
//
//    private void generateOutput() {
//        output.append("=== 词法分析结果 ===\n");
//        output.append("Tokens:\n");
//        tokens.forEach(t -> output.append(t.toFormattedString()).append("\n"));
//
//        output.append("\n=== 符号表 ===\n");
//        output.append(symbolTable.getFormattedTable());
//
//        output.append("\n=== 错误报告 ===\n");
//        output.append(errorCollector.getErrorReport());
//    }
//
//    // 其他辅助方法省略...
//}
//
//// 优化后的词法单元定义
//class WordList {
//    public static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
//            "int", "float", "char", "string", "if", "else", "while",
//            "for", "return", "void", "break", "continue", "switch",
//            "case", "default", "do", "goto", "const", "static"
//    ));
//
//    public static final Set<String> OPERATORS = new HashSet<>(Arrays.asList(
//            "+", "-", "*", "/", "=", "==", "!=", "<", ">", "<=", ">=",
//            "&&", "||", "!", "++", "--", "+=", "-=", "*=", "/=", "?:",
//            "&", "|", "^", "<<", ">>", "%", "->", ".", "...", "~"
//    ));
//
//    public static final Set<String> DELIMITERS = new HashSet<>(Arrays.asList(
//            ";", ",", "(", ")", "{", "}", "[", "]", "#", "\\", ":", "?"
//    ));
//}