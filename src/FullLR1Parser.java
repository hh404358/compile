/**
 * @Author: hahaha
 * @Package:PACKAGE_NAME
 * @Project:word_translator
 * @name:AnalyseTable
 * @Date:2025/4/27
 * @Time:17:28
 * @Filename:AnalyseTable
 **/

import java.io.FileOutputStream;
import java.util.*;

public class FullLR1Parser {

    // 数据结构定义
    static class Production {
        String lhs;
        String[] rhs;
        int id;

        Production(String lhs, String[] rhs, int id) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.id = id;
        }
    }

    static class LR1Item {
        Production prod;
        int dot;
        Set<String> lookaheads;

        LR1Item(Production prod, int dot, Set<String> lookaheads) {
            this.prod = prod;
            this.dot = dot;
            this.lookaheads = new LinkedHashSet<>(lookaheads);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LR1Item item = (LR1Item) o;
            return dot == item.dot &&
                    prod.id == item.prod.id &&
                    lookaheads.equals(item.lookaheads);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prod.id, dot, lookaheads);
        }
    }

    // 全局变量
    static List<Production> productions = new ArrayList<>();
    static Map<String, Set<String>> firstSets = new HashMap<>();
    static Map<Integer, Map<String, String>> actionTable = new HashMap<>();
    static Map<Integer, Map<String, Integer>> gotoTable = new HashMap<>();
    static List<Set<LR1Item>> states = new ArrayList<>();
    static Set<String> terminals = new LinkedHashSet<>(Arrays.asList(
            "{", "}", ";", "id", "num", "real", "true", "false", "(", ")",
            "||", "&&", "==", "!=", "<", "<=", ">", ">=", "+", "-", "*", "/",
            "!", "-", "=", "if", "else", "while", "do", "break","loc", "int", "float", "bool","[","$"

    ));
    static Set<String> nonTerminals = new LinkedHashSet<>(Arrays.asList(
            "program", "block", "decls", "decl", "type", "stmts", "stmt",
            "loc", "bool", "join", "equality", "rel", "expr", "term", "unary", "factor","basic"
    ));

    public static void main(String[] args) throws Exception {
        initializeProductions();
        computeFirstSets();
        buildParser();
        printAnalysisTables();
//        exportToExcel();
    }

    // 初始化产生式
    static void initializeProductions() {
        // 程序结构
        productions.add(new Production("program", new String[]{"block"}, 0));

        // 块结构
        productions.add(new Production("block", new String[]{"{", "decls", "stmts", "}"}, 1));

        // 声明部分
        productions.add(new Production("decls", new String[]{"decls", "decl"}, 2));
        productions.add(new Production("decls", new String[]{}, 3)); // ε产生式

        // 单个声明
        productions.add(new Production("decl", new String[]{"type", "id", ";"}, 4));

        // 类型定义
        productions.add(new Production("type", new String[]{"type", "[", "num", "]"}, 5));
        productions.add(new Production("type", new String[]{"basic"}, 6));


        // 语句序列
        productions.add(new Production("stmts", new String[]{"stmts", "stmt"}, 7));
        productions.add(new Production("stmts", new String[]{}, 8)); // ε产生式

        // 语句类型
        productions.add(new Production("stmt", new String[]{"loc", "=", "bool", ";"}, 9));       // loc=bool;
        productions.add(new Production("stmt", new String[]{"if", "(", "bool", ")", "stmt"}, 10)); // if(bool)stmt
        productions.add(new Production("stmt", new String[]{"if", "(", "bool", ")", "stmt", "else", "stmt"}, 11)); // if-else
        productions.add(new Production("stmt", new String[]{"while", "(", "bool", ")", "stmt"}, 12)); // while(bool)stmt
        productions.add(new Production("stmt", new String[]{"do", "stmt", "while", "(", "bool", ")", ";"}, 13)); // do-while
        productions.add(new Production("stmt", new String[]{"break", ";"}, 14)); // break;
        productions.add(new Production("stmt", new String[]{"block"}, 15)); // block

        // 左值
        productions.add(new Production("loc", new String[]{"loc", "[", "num", "]"}, 16));
        productions.add(new Production("loc", new String[]{"id"}, 17));

        // 布尔表达式
        productions.add(new Production("bool", new String[]{"bool", "||", "join"}, 18));
        productions.add(new Production("bool", new String[]{"join"}, 19));

        // 逻辑与
        productions.add(new Production("join", new String[]{"join", "&&", "equality"}, 20));
        productions.add(new Production("join", new String[]{"equality"}, 21));

        // 相等判断
        productions.add(new Production("equality", new String[]{"equality", "==", "rel"}, 22));
        productions.add(new Production("equality", new String[]{"equality", "!=", "rel"}, 23));
        productions.add(new Production("equality", new String[]{"rel"}, 24));

        // 关系表达式
        productions.add(new Production("rel", new String[]{"expr", "<", "expr"}, 25));
        productions.add(new Production("rel", new String[]{"expr", "<=", "expr"}, 26));
        productions.add(new Production("rel", new String[]{"expr", ">=", "expr"}, 27));
        productions.add(new Production("rel", new String[]{"expr", ">", "expr"}, 28));
        productions.add(new Production("rel", new String[]{"expr"}, 29));

        // 算术表达式
        productions.add(new Production("expr", new String[]{"expr", "+", "term"}, 30));
        productions.add(new Production("expr", new String[]{"expr", "-", "term"}, 31));
        productions.add(new Production("expr", new String[]{"term"}, 32));

        // 项
        productions.add(new Production("term", new String[]{"term", "*", "unary"}, 33));
        productions.add(new Production("term", new String[]{"term", "/", "unary"}, 34));
        productions.add(new Production("term", new String[]{"unary"}, 35));

        // 一元表达式
        productions.add(new Production("unary", new String[]{"!", "unary"}, 36));
        productions.add(new Production("unary", new String[]{"-", "unary"}, 37));
        productions.add(new Production("unary", new String[]{"factor"}, 38));

        // 基本因子
        productions.add(new Production("factor", new String[]{"(", "bool", ")"}, 39));
        productions.add(new Production("factor", new String[]{"loc"}, 40));
        productions.add(new Production("factor", new String[]{"num"}, 41));
        productions.add(new Production("factor", new String[]{"real"}, 42));
        productions.add(new Production("factor", new String[]{"true"}, 43));
        productions.add(new Production("factor", new String[]{"false"}, 44));

        // 基本类型
        productions.add(new Production("basic", new String[]{"int"}, 45));
        productions.add(new Production("basic", new String[]{"float"}, 46));
        productions.add(new Production("basic", new String[]{"bool"}, 47));

    }

    // FIRST集计算
    static void computeFirstSets() {
        // 确保所有符号已初始化
        firstSets.clear();

        // 初始化终结符的FIRST集
        for (String t : terminals) {
            firstSets.put(t, new LinkedHashSet<>(Collections.singleton(t)));
        }

        // 初始化非终结符的FIRST集
        for (String nt : nonTerminals) {
            firstSets.put(nt, new LinkedHashSet<>());
        }

        boolean changed;
        do {
            changed = false;
            for (Production p : productions) {
                // 动态处理未识别的符号
                if (!firstSets.containsKey(p.lhs)) {
                    firstSets.put(p.lhs, new LinkedHashSet<>());
                }

                Set<String> rhsFirst = computeRhsFirst(p.rhs, 0);
                Set<String> currentFirst = firstSets.get(p.lhs);

                // 仅添加新元素
                for (String s : rhsFirst) {
                    if (!currentFirst.contains(s)) {
                        currentFirst.add(s);
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    static Set<String> computeRhsFirst(String[] symbols, int index) {
        if (index >= symbols.length) {
            return new LinkedHashSet<>(Collections.singleton("ε"));
        }

        String symbol = symbols[index];

        // 动态处理未识别的符号
        if (!firstSets.containsKey(symbol)) {
            System.err.println("警告: 未定义符号 '" + symbol + "'");
            firstSets.put(symbol, new LinkedHashSet<>());
        }

        Set<String> first = new LinkedHashSet<>(firstSets.get(symbol));

        // 处理ε情况
        if (first.contains("ε")) {
            first.remove("ε");
            Set<String> nextFirst = computeRhsFirst(symbols, index + 1);
            first.addAll(nextFirst);
        }
        return first;
    }

    // 项集闭包计算
    static Set<LR1Item> closure(Set<LR1Item> items) {
        Set<LR1Item> closure = new LinkedHashSet<>(items);
        boolean changed;
        do {
            changed = false;
            Set<LR1Item> toAdd = new LinkedHashSet<>();
            for (LR1Item item : closure) {
                String[] rhs = item.prod.rhs;
                if (item.dot < rhs.length) {
                    String B = rhs[item.dot];
                    if (nonTerminals.contains(B)) {
                        String[] beta = Arrays.copyOfRange(rhs, item.dot + 1, rhs.length);
                        Set<String> firstBeta = computeRhsFirst(beta, 0);
                        firstBeta.remove("ε");
                        if (firstBeta.isEmpty()) firstBeta.addAll(item.lookaheads);

                        for (Production p : productions) {
                            if (p.lhs.equals(B)) {
                                LR1Item newItem = new LR1Item(p, 0, firstBeta);
                                if (!closure.contains(newItem) && !toAdd.contains(newItem)) {
                                    toAdd.add(newItem);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
            closure.addAll(toAdd);
        } while (changed);
        return closure;
    }

    // GOTO函数实现
    static Set<LR1Item> goTo(Set<LR1Item> state, String X) {
        Set<LR1Item> gotoSet = new LinkedHashSet<>();
        for (LR1Item item : state) {
            String[] rhs = item.prod.rhs;
            if (item.dot < rhs.length && rhs[item.dot].equals(X)) {
                gotoSet.add(new LR1Item(item.prod, item.dot + 1, item.lookaheads));
            }
        }
        return closure(gotoSet);
    }

    // 构建分析器
    static void buildParser() {
        // 初始化初始状态
        Set<LR1Item> initial = closure(new LinkedHashSet<>(Collections.singleton(
                new LR1Item(productions.get(0), 0, new LinkedHashSet<>(Collections.singleton("$")))
        )));
        states.add(initial);

        // 构建状态转移
        Queue<Integer> queue = new LinkedList<>();
        queue.add(0);

        while (!queue.isEmpty()) {
            int stateId = queue.poll();
            Set<LR1Item> currentState = states.get(stateId);

            // 合并所有可能的符号
            Set<String> allSymbols = new LinkedHashSet<>(terminals);
            allSymbols.addAll(nonTerminals);

            for (String symbol : allSymbols) {
                Set<LR1Item> newState = goTo(currentState, symbol);
                if (!newState.isEmpty()) {
                    int existing = findState(newState);
                    if (existing == -1) {
                        states.add(newState);
                        existing = states.size() - 1;
                        queue.add(existing);
                    }

                    if (terminals.contains(symbol)) {
                        actionTable.computeIfAbsent(stateId, k -> new HashMap<>())
                                .put(symbol, "s" + existing);
                    } else {
                        gotoTable.computeIfAbsent(stateId, k -> new HashMap<>())
                                .put(symbol, existing);
                    }
                }
            }

            // 处理归约项
            for (LR1Item item : currentState) {
                if (item.dot == item.prod.rhs.length) {
                    for (String la : item.lookaheads) {
                        String action = "r" + item.prod.id;

                        // 检查是否为else冲突
                        if (la.equals("else")) {
                            // 获取可能的移进目标状态
                            Integer shiftState = getShiftState(stateId, "else");
                            if (shiftState != null) {
                                // 强制选择移进（解决dangling-else）
                                action = "s" + shiftState;
                                System.out.println("应用else优先级: 状态" + stateId + " 强制移进到" + shiftState);
                            }
                        }

                        // 冲突处理逻辑
                        if (actionTable.containsKey(stateId) &&
                                actionTable.get(stateId).containsKey(la)) {
                            String existingAction = actionTable.get(stateId).get(la);

                            // 特殊处理else冲突
                            if (la.equals("else")) {
                                System.err.println("[已解决] 冲突在状态 " + stateId + " 符号 else" +
                                        "，应用移进优先规则");
                                continue; // 跳过默认的覆盖操作
                            } else {
                                System.err.println("未解决冲突在状态 " + stateId + " 符号 " + la +
                                        " 原有动作:" + existingAction + " 新动作:" + action);
                            }
                        }

                        actionTable.computeIfAbsent(stateId, k -> new HashMap<>())
                                .put(la, action);
                    }
                }
            }
        }
    }


    // 新增方法：获取指定符号的移进状态
    private static Integer getShiftState(int currentState, String symbol) {
        Set<LR1Item> stateItems = states.get(currentState);
        for (LR1Item item : stateItems) {
            if (item.dot < item.prod.rhs.length &&
                    item.prod.rhs[item.dot].equals(symbol)) {
                // 查找该符号的GOTO状态
                return findState(goTo(stateItems, symbol));
            }
        }
        return null;
    }
    // 查找状态
    static int findState(Set<LR1Item> target) {
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i).equals(target)) return i;
        }
        return -1;
    }

    // 控制台输出实现
    static void printAnalysisTables() {
        System.out.println("\n============ ACTION表 ============");
        printActionTable();

        System.out.println("\n============ GOTO表 ==============");
        printGotoTable();
    }

    static void printActionTable() {
        // 打印表头
        System.out.printf("%-6s", "State");
        for (String t : terminals) {
            System.out.printf("%-8s", shortenSymbol(t));
        }
        System.out.println();

        // 打印内容
        for (int state = 0; state < states.size(); state++) {
            System.out.printf("%-6d", state);
            for (String t : terminals) {
                String action = actionTable.getOrDefault(state, Collections.emptyMap()).get(t);
                System.out.printf("%-8s", action != null ? action : "");
            }
            System.out.println();

//            // 每20行暂停
//            if ((state + 1) % 20 == 0) {
//                System.out.println("-- 按Enter继续 --");
//                new Scanner(System.in).nextLine();
//            }
        }
    }

    static void printGotoTable() {
        // 打印表头
        System.out.printf("%-6s", "State");
        for (String nt : nonTerminals) {
            System.out.printf("%-8s", shortenSymbol(nt));
        }
        System.out.println();

        // 打印内容
        for (int state = 0; state < states.size(); state++) {
            System.out.printf("%-6d", state);
            for (String nt : nonTerminals) {
                Integer gotoState = gotoTable.getOrDefault(state, Collections.emptyMap()).get(nt);
                System.out.printf("%-8s", gotoState != null ? gotoState : "");
            }
            System.out.println();

//            // 每20行暂停
//            if ((state + 1) % 20 == 0) {
//                System.out.println("-- 按Enter继续 --");
//                new Scanner(System.in).nextLine();
//            }
        }
    }

    // 符号缩写处理（用于控制台显示）
    static String shortenSymbol(String symbol) {
        if (symbol.length() > 5) {
            return symbol.substring(0, 3) + "..";
        }
        return symbol;
    }
}