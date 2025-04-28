//import java.util.*;
//
//// 符号类型（终结符和非终结符）
//enum Symbol {
//    // ================ 终结符 ================
//    // 括号与分隔符
//    LBRACE("{"), RBRACE("}"), LPAREN("("), RPAREN(")"), SEMI(";"),
//    SQUARE_LBRACKET("["), SQUARE_RBRACKET("]"),
//
//    // 关键字
//    IF("if"), ELSE("else"), WHILE("while"), DO("do"), BREAK("break"),
//    BASIC("basic"), TRUE("true"), FALSE("false"),
//
//    // 标识符与字面量
//    ID("id"), NUM("num"), REAL("real"),
//
//    // 运算符
//    EQUALS("="), OR("||"), AND("&&"),
//    EQ("=="), NE("!="), LT("<"), LE("<="), GT(">"), GE(">="),
//    PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), NOT("!"),
//
//    // 特殊符号
//    EOF("$"),
//
//    // ================ 非终结符 ================
//    // 程序结构
//    PROGRAM,      // 程序入口
//    BLOCK,        // 代码块 { ... }
//    DECLS,        // 声明序列
//    DECL,         // 单个声明
//    TYPE,         // 类型
//
//    // 语句相关
//    STMTS,        // 语句序列
//    STMT,         // 单条语句
//    LOC,          // 左值 (loc[num] 或 id)
//
//    // 布尔表达式
//    BOOL,         // 布尔表达式
//    JOIN,         // 逻辑与表达式
//    EQUALITY,     // 相等性判断
//
//    // 关系与算术
//    REL,          // 关系表达式
//    EXPR,         // 算术表达式
//    TERM,         // 项
//    UNARY,        // 一元表达式
//    FACTOR;       // 基本因子
//
//    // 添加终结符的字符串表示
//    private final String lexeme;
//
//    Symbol() { this.lexeme = null; }          // 非终结符构造
//    Symbol(String lexeme) { this.lexeme = lexeme; } // 终结符构造
//
//    public boolean isTerminal() {
//        return lexeme != null; // 终结符有lexeme值
//    }
//
//    public String getLexeme() {
//        return lexeme != null ? lexeme : name(); // 非终结符返回枚举名称
//    }
//}
//
//class Action {
//    enum Type { SHIFT, REDUCE, ERROR, ACCEPT }
//    Type type;
//    int value;
//
//    Action(Type type, int value) {
//        this.type = type;
//        this.value = value;
//    }
//}
//
//
//// 产生式定义
//class Production {
//    Symbol lhs;
//    List<Symbol> rhs;
//
//    Production(Symbol lhs, List<Symbol> rhs) {
//        this.lhs = lhs;
//        this.rhs = Collections.unmodifiableList(new ArrayList<>(rhs));
//    }
//}
//public class LRParser {
//    // 动作表和GOTO表
//    Map<Integer, Map<Symbol, Action>> actionTable = new HashMap<>();
//    Map<Integer, Map<Symbol, Integer>> gotoTable = new HashMap<>();
//    private Stack<Integer> states = new Stack<>();
//    private Stack<Symbol> symbols = new Stack<>();
//    private List<Symbol> inputs;
//    private List<Production> productions = Arrays.asList(
//            new Production(Symbol.PROGRAM, Arrays.asList(Symbol.BLOCK)),
//            new Production(Symbol.BLOCK, Arrays.asList(Symbol.LBRACE, Symbol.DECLS, Symbol.STMTS, Symbol.RBRACE)),
//            new Production(Symbol.DECLS, Arrays.asList(Symbol.DECLS, Symbol.DECL)),
//            new Production(Symbol.DECLS, Collections.emptyList()),
//            new Production(Symbol.DECL, Arrays.asList(Symbol.TYPE, Symbol.ID, Symbol.SEMI)),
//            new Production(Symbol.TYPE, Arrays.asList(Symbol.BASIC)),
//            new Production(Symbol.TYPE, Arrays.asList(Symbol.REAL)),
//            new Production(Symbol.STMTS, Arrays.asList(Symbol.STMTS, Symbol.STMT)),
//            new Production(Symbol.STMTS, Collections.emptyList()),
//            new Production(Symbol.STMT, Arrays.asList(Symbol.LOC, Symbol.EQUALS, Symbol.EXPR, Symbol.SEMI)),
//            new Production(Symbol.STMT, Arrays.asList(Symbol.IF, Symbol.LPAREN, Symbol.BOOL, Symbol.RPAREN, Symbol.STMT)),
//            new Production(Symbol.STMT, Arrays.asList(Symbol.IF, Symbol.LPAREN, Symbol.BOOL, Symbol.RPAREN, Symbol.STMT, Symbol.ELSE, Symbol.STMT)),
//            new Production(Symbol.STMT, Arrays.asList(Symbol.WHILE, Symbol.LPAREN, Symbol.BOOL, Symbol.RPAREN, Symbol.STMT)),
//            new Production(Symbol.STMT, Arrays.asList(Symbol.DO, Symbol.STMT, Symbol.WHILE, Symbol.LPAREN, Symbol.BOOL, Symbol.RPAREN, Symbol.SEMI)),
//            new Production(Symbol.STMT, Arrays.asList(Symbol.BREAK, Symbol.SEMI)),
//            new Production(Symbol.LOC, Arrays.asList(Symbol.ID)),
//            new Production(Symbol.BOOL, Arrays.asList(Symbol.BOOL, Symbol.OR, Symbol.JOIN)),
//            new Production(Symbol.BOOL, Arrays.asList(Symbol.JOIN)),
//            new Production(Symbol.JOIN, Arrays.asList(Symbol.JOIN, Symbol.AND, Symbol.EQUALITY)),
//            new Production(Symbol.JOIN, Arrays.asList(Symbol.EQUALITY)),
//            new Production(Symbol.EQUALITY, Arrays.asList(Symbol.EQUALITY, Symbol.EQ, Symbol.REL)),
//            new Production(Symbol.EQUALITY, Arrays.asList(Symbol.EQUALITY, Symbol.NE, Symbol.REL)),
//            new Production(Symbol.EQUALITY, Arrays.asList(Symbol.REL)),
//            new Production(Symbol.REL, Arrays.asList(Symbol.REL, Symbol.LT, Symbol.EXPR)),
//            new Production(Symbol.REL, Arrays.asList(Symbol.REL, Symbol.LE, Symbol.EXPR)),
//            new Production(Symbol.REL, Arrays.asList(Symbol.REL, Symbol.GT, Symbol.EXPR)),
//            new Production(Symbol.REL, Arrays.asList(Symbol.REL, Symbol.GE, Symbol.EXPR)),
//            new Production(Symbol.REL, Arrays.asList(Symbol.EXPR)),
//            new Production(Symbol.EXPR, Arrays.asList(Symbol.EXPR, Symbol.PLUS, Symbol.TERM)),
//            new Production(Symbol.EXPR, Arrays.asList(Symbol.EXPR, Symbol.MINUS, Symbol.TERM)),
//            new Production(Symbol.EXPR, Arrays.asList(Symbol.TERM)),
//            new Production(Symbol.TERM, Arrays.asList(Symbol.TERM, Symbol.TIMES, Symbol.UNARY)),
//            new Production(Symbol.TERM, Arrays.asList(Symbol.TERM, Symbol.DIV, Symbol.UNARY)),
//            new Production(Symbol.TERM, Arrays.asList(Symbol.UNARY)),
//            new Production(Symbol.UNARY, Arrays.asList(Symbol.PLUS, Symbol.UNARY)),
//            new Production(Symbol.UNARY, Arrays.asList(Symbol.MINUS, Symbol.UNARY)),
//            new Production(Symbol.UNARY, Arrays.asList(Symbol.NOT, Symbol.UNARY)),
//            new Production(Symbol.UNARY, Arrays.asList(Symbol.FACTOR)),
//            new Production(Symbol.FACTOR, Arrays.asList(Symbol.NUM)),
//            new Production(Symbol.FACTOR, Arrays.asList(Symbol.ID)),
//            new Production(Symbol.FACTOR, Arrays.asList(Symbol.TRUE)),
//            new Production(Symbol.FACTOR, Arrays.asList(Symbol.FALSE)),
//            new Production(Symbol.FACTOR, Arrays.asList(Symbol.LPAREN, Symbol.EXPR, Symbol.RPAREN))
//    );
//    private int pos = 0;
//
//    public LRParser(List<Symbol> inputs) {
//        this.inputs = new ArrayList<>(inputs);
//        this.inputs.add(Symbol.EOF);
//        states.push(0);
//    }
//
//    public void parse() {
//        while (true) {
//            int state = states.peek();
//            Symbol input = inputs.get(pos);
//
//            Action action = actionTable.getOrDefault(state, Collections.emptyMap())
//                    .getOrDefault(input, new Action(Action.Type.ERROR, -1));
//
//            printState(); // 输出当前栈状态
//
//            switch (action.type) {
//                case SHIFT:
//                    states.push(action.value);
//                    symbols.push(input);
//                    pos++;
//                    break;
//                case REDUCE:
//                    Production prod = productions.get(action.value);
//                    int popCount = prod.rhs.size();
//                    // 处理ε产生式
//                    if (popCount > 0) {
//                        states.subList(states.size()-popCount, states.size()).clear();
//                        symbols.subList(symbols.size()-popCount, symbols.size()).clear();
//                    }
//                    symbols.push(prod.lhs);
//                    states.push(gotoTable.get(states.peek()).get(prod.lhs));
//                    break;
//                case ACCEPT:
//                    System.out.println("Accept");
//                    return;
//                default:
//                    throw new RuntimeException("Syntax error");
//            }
//        }
//    }
//    private void printState() {
//        System.out.println("分析栈:");
//        System.out.println("状态栈: " + states);
//        System.out.println("符号栈: " + symbols);
//        System.out.println("剩余输入: " + inputs.subList(pos, inputs.size()));
//        System.out.println("------------------------");
//    }
//
//    public void printAnalysisTable() {
//        System.out.println("语法分析表:");
//        System.out.println("ACTION表:");
//        actionTable.forEach((state, actions) ->
//                actions.forEach((sym, act) ->
//                        System.out.printf("State %2d | Symbol %-8s → %s%n",
//                                state, sym, act.type + (act.type==Action.Type.SHIFT ? ""+act.value : ""))
//                ));
//
//        System.out.println("\nGOTO表:");
//        gotoTable.forEach((state, trans) ->
//                trans.forEach((nt, s) ->
//                        System.out.printf("State %2d | NT %-8s → %d%n", state, nt, s)));
//    }
//    public static void main(String[] args) {
//        // 构造测试输入流
//        List<Symbol> input = Arrays.asList(
//                Symbol.LBRACE,
//                Symbol.RBRACE,
//                Symbol.EOF
//        );
//
//        LRParser parser = new LRParser(input);
//        parser.printAnalysisTable();
//        parser.parse();
//    }
//}
