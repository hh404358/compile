import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Lx、Zqy
 * @Package: PACKAGE_NAME
 * @Project: compile
 * @name: TokenToSymbolConverter
 * @Date: 2025/4/27 16:43
 * @Filename: TokenToSymbolConverter
 */
public class TokenToSymbolConverter {
    /**
     * 将Token列表转换为SymbolDeclaration列表
     * @param tokens 输入的Token列表
     * @return 转换后的SymbolDeclaration列表
     */
    public List<SymbolDeclaration> convert(List<Token> tokens) {
        List<SymbolDeclaration> symbolTable = new ArrayList<>();

        for (Token token : tokens) {
            Symbol symbol = determineSymbol(token);
            symbolTable.add(new SymbolDeclaration(
                    symbol,
                    token.getLine(),
                    token.getPosition()
            ));
        }
        return symbolTable;
    }
    /**
     * 根据Token确定Symbol
     * @param token Token的值
     * @return 对应的Symbol
     */
    public Symbol determineSymbol(Token token) {
        if (token.value == null) {
            throw new IllegalArgumentException("Token value cannot be null");
        }
        if (token.type == TokenType.IDENTIFIER) {
            return Symbol.ID;
        } else if (token.type == TokenType.NUMERIC_CONST) {
            return Symbol.NUM;
        }
        switch (token.value) {
            // 处理基本类型声明符
            case "int": case "float": case "bool": return Symbol.BASIC;
            // 处理关键字
            case "if": return Symbol.IF;
            case "else": return Symbol.ELSE;
            case "while": return Symbol.WHILE;
            case "do": return Symbol.DO;
            case "break": return Symbol.BREAK;
            case "true": return Symbol.TRUE;
            case "false": return Symbol.FALSE;
            // 处理运算符和分隔符
            case "{": return Symbol.LBRACE;
            case "}": return Symbol.RBRACE;
            case "(": return Symbol.LPAREN;
            case ")": return Symbol.RPAREN;
            case "[": return Symbol.SQUARE_LBRACKET;
            case "]": return Symbol.SQUARE_RBRACKET;
            case ";": return Symbol.SEMI;
            case "=": return Symbol.EQUALS;
            case "||": return Symbol.OR;
            case "&&": return Symbol.AND;
            case "==": return Symbol.EQ;
            case "!=": return Symbol.NE;
            case "<": return Symbol.LT;
            case "<=": return Symbol.LE;
            case ">": return Symbol.GT;
            case ">=": return Symbol.GE;
            case "+": return Symbol.PLUS;
            case "-": return Symbol.MINUS;
            case "*": return Symbol.TIMES;
            case "/": return Symbol.DIV;
            case "!": return Symbol.NOT;
            case "$": return Symbol.EOF;
        }
        throw new IllegalArgumentException("Unrecognized token value: " + token.value);
    }
}
