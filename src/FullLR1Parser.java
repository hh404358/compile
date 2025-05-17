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

class ParseStep {
    List<Integer> states;
    List<String> symbols;
    List<String> input;
    String action;
    ParseStep(List<Integer> states, List<String> symbols, List<String> input, String action) {
        this.states = new ArrayList<>(states);
        this.symbols = new ArrayList<>(symbols);
        this.input = new ArrayList<>(input);
        this.action = action;
    }
}

public class FullLR1Parser {

    // 中间代码
    static List<IntermediateCode> intermediateCode = new ArrayList<>();
    // 语义分析错误
    static List<String> SemanticErrors = new ArrayList<>();
    static Stack<String> valueStack= new Stack<>();
    static SymbolTable symbolTable = new SymbolTable();
    static NumTable numTable = new NumTable();
    static Map<String, String> arrayAddrOrigin = new HashMap<>(); // tX -> arr

    // 循环结束栈
    static Stack<String> breakLabelStack = new Stack<>();

    static String result;

    static int tempVarCount=0;
    public static List<IntermediateCode> getIntermediateCode() {
        return intermediateCode;
    }

    // 给前端的语义错误接口
    public static List<String> getSemanticErrors() {
        return SemanticErrors;
    }

    // 数据结构定义
    static class Production {
        String lhs;
        String[] rhs;
        int id;// 产生式


        Production(String lhs, String[] rhs, int id) {
            this.lhs = lhs;
            this.rhs = rhs;
            this.id = id;
        }

        /**
         * 根据产生式规则生成中间代码
         * @param valueStack
         * @return
         */
        List<IntermediateCode> generateCode(Stack<String> valueStack, Stack<String>symbolStack,int line, int position) {
            List<IntermediateCode> code = new ArrayList<>();
            switch (id) {
                // decl → type id;
                case 4:
                    // 值栈内容: [type, id, ;]
                    valueStack.pop(); // 弹出;
                    String idVal = valueStack.pop();      // id
                    String typeVal = valueStack.pop();     // type
                    if(typeVal.equals("]")) {
                        while (!valueStack.peek().equals("[")) {
                            valueStack.pop();
                        }
                        valueStack.pop();
                        typeVal = valueStack.pop() + "*";
                    }

                    // 检查idVal是否重复声明
                    if (symbolTable.contains(idVal)) {
                        SemanticErrors.add(generateSemanticError("变量" + idVal + "重复声明", line, position));
                        break;
                    }

                    // 记录被声明的值和类型
                    symbolTable.insert(idVal, typeVal);

                    // 声明指令，结果存符号表
                    code.add(new IntermediateCode("DECLARE", typeVal, idVal, null));

                    break;

                // type → basic
                case 6:
                    String basic = valueStack.pop(); // basic
                    // 类型直接传递，无需临时变量
                    valueStack.push(basic);
                    break;
                // stmt → loc = bool;
                case 9:
                    valueStack.pop(); // ;
                    String boolValue = valueStack.pop(); // bool
                    valueStack.pop(); // =
                    String loc = valueStack.pop(); // loc

                    String locType = null;

                    // 判断是否数组地址
                    if (loc.startsWith("t") && arrayAddrOrigin.containsKey(loc)) {
                        // 是数组元素地址，查找原始数组名并获取元素类型
                        String arrayName = arrayAddrOrigin.get(loc);
                        if (!symbolTable.contains(arrayName)) {
                            break;
                        }
                        locType = symbolTable.lookupType(arrayName);
                        // 如果是数组类型如 "int*", 去掉指针符号
                        if (locType.endsWith("*")) {
                            locType = locType.substring(0, locType.length() - 1);
                        }
                    } else {
                        if (!symbolTable.contains(loc)) {
                            break;
                        }
                        // 不用重复变量声明检查，因为已经在loc产生式中检查过了
                        locType = symbolTable.lookupType(loc);
                    }

                    // 类型检查
                    String boolValueType;
                    if (locType.equals("int") && boolValue.equals("true")) {
                        boolValue = "1";
                        boolValueType = "boolean";
                    } else if (locType.equals("int") && boolValue.equals("false")) {
                        boolValue = "0";
                        boolValueType = "boolean";
                    } else if (locType.equals("float") && (boolValue.equals("true") || boolValue.equals("false"))) {
                        SemanticErrors.add(generateSemanticError("boolean无法赋值给float变量", line, position));
                        break;
                    }

                    // 生成赋值代码
                    if (loc.startsWith("t") && arrayAddrOrigin.containsKey(loc)) {
                        // 数组赋值：STORE value -> *address
                        code.add(new IntermediateCode("STORE", boolValue, null, loc));
                    } else {
                        // 普通变量赋值
                        code.add(new IntermediateCode("ASSIGN", loc, null, boolValue));
                    }

                    break;

                // if (bool) stmt
                case 10:
                    // 值栈内容: [if, (, bool, ), stmt]
                    if(valueStack.peek().equals("}"))valueStack.pop(); // 弹出}
                    if(valueStack.peek().equals("{"))valueStack.pop(); // 弹出}
                    if(valueStack.peek().equals(")"))valueStack.pop(); // 弹出)
                    String boolVal = valueStack.pop();   // bool值
                    if(valueStack.peek().equals("("))valueStack.pop(); // 弹出(
                    String ifKeyword = valueStack.pop(); // 弹出if

                    String trueLabel = "L" + labelCount++;
                    String endLabel = "L" + labelCount++;
                    if (boolVal.contains("L")) {
                        break;
                    }
                    // 条件跳转指令
                    List<IntermediateCode>tempList = new ArrayList<>();

                    List<IntermediateCode> tempList1 = new ArrayList<>();
                    //语句2
                    while(!intermediateCode.isEmpty() && (intermediateCode.get(intermediateCode.size() - 1).getResult() == null || !intermediateCode.get(intermediateCode.size() - 1).getOperator().equals("LOAD"))){
                        tempList.add(intermediateCode.get(intermediateCode.size() - 1));
                        intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                    }
                    if(!intermediateCode.isEmpty() && intermediateCode.get(intermediateCode.size() - 1).getOperator().equals("LOAD")){
                        tempList.add(intermediateCode.get(intermediateCode.size() - 1));
                        intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                    }
                    //语句1
                    while(!intermediateCode.isEmpty() && (intermediateCode.get(intermediateCode.size() - 1).getOperator() == null ||
                            !intermediateCode.get(intermediateCode.size() - 1).getOperator().equals("LOAD") &&
                                    !intermediateCode.get(intermediateCode.size() - 1).getOperator().equals("LABEL"))
                    ){
                        tempList1.add(intermediateCode.get(intermediateCode.size() - 1));
                        intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                    }
                    if(!intermediateCode.isEmpty() && intermediateCode.get(intermediateCode.size() - 1).getOperator().equals("LOAD")){
                        tempList1.add(intermediateCode.get(intermediateCode.size() - 1));
                        intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                    }
                    if(!intermediateCode.isEmpty() && intermediateCode.get(intermediateCode.size()-1).getOperator().equals("LABEL")){

                        //true
                        IntermediateCode falseLabel = intermediateCode.get(intermediateCode.size() - 1);
                        intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                        IntermediateCode trueLabel1 = intermediateCode.get(intermediateCode.size()-1);
                        intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                        IntermediateCode false1 = intermediateCode.get(intermediateCode.size() - 1);
                        intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                        IntermediateCode true1=intermediateCode.get(intermediateCode.size()-1);
                        intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                        IntermediateCode true2=intermediateCode.get(intermediateCode.size()-1);
                        intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                        //right's
                        List<IntermediateCode>right = new ArrayList<>();
                        while(!intermediateCode.isEmpty() && (intermediateCode.get(intermediateCode.size() - 1).getOperator() == null ||
                                !intermediateCode.get(intermediateCode.size() - 1).getOperator().equals("LOAD"))
                                && !intermediateCode.get(intermediateCode.size() - 1).getOperator().equals("LABEL")
                        ){
                            right.add(intermediateCode.get(intermediateCode.size() - 1));
                            intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                        }
                        if(!intermediateCode.isEmpty()&&intermediateCode.get(intermediateCode.size()-1).getOperator().equals("LOAD")){
                            right.add(intermediateCode.get(intermediateCode.size() - 1));
                            intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                        }
                        code.add(true2);
                        Collections.reverse(right);
                        code.addAll(right);
                        code.add(true1);
                        if(tempList1.isEmpty()){
                            Collections.reverse(tempList);
                            code.addAll(tempList);
                            code.add(false1);
                            code.add(falseLabel);

                        }else{
                            code.add(trueLabel1);
                            Collections.reverse(tempList1);
                            code.addAll(tempList1);
                            //false
                            code.add(false1);
                            code.add(falseLabel);
                            Collections.reverse(tempList);
                            code.addAll(tempList);
                        }

                        endLabel = falseLabel.getArg1();
                        valueStack.push(endLabel);
                    }else{
                        Collections.reverse(tempList1);
                        code.addAll(tempList1);
                        code.add(new IntermediateCode("IF_FALSE", boolVal, "GOTO " + endLabel, null));
                        code.add(new IntermediateCode("LABEL", trueLabel, null, null));
                        Collections.reverse(tempList);
                        code.addAll(tempList);
                        code.add(new IntermediateCode("LABEL", endLabel, null, null));

                        // 压入结束标签供后续回填
//                        valueStack.push(endLabel);
                    }

                    break;
                // if-else结构
                case 11:
                    // 处理else部分
                    String end = valueStack.pop();
                    String start = valueStack.pop();
                    symbolStack.pop();//弹出

                    if(valueStack.peek().equals("else")){
                        valueStack.pop();//else
//                        String elseLabel = "L" + labelCount++;
                        endLabel = "L" + labelCount++;
                        end= valueStack.pop();
                        start = valueStack.pop();
                        String rparen = valueStack.pop();//(
                        boolVal = valueStack.pop();
                        tempList = new ArrayList<>();
                        // 条件跳转指令
                        while(!intermediateCode.isEmpty() && (intermediateCode.get(intermediateCode.size() - 1).getResult() == null || !intermediateCode.get(intermediateCode.size() - 1).getResult().equals(boolVal))){
                            tempList.add(intermediateCode.get(intermediateCode.size() - 1));
                            intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                        }
                        code.add(new IntermediateCode("IF_FALSE", boolVal, "GOTO " + endLabel, null));
                        while(tempList.get(tempList.size() - 1).getOperator()!=null && !tempList.get(tempList.size() - 1).getOperator().equals("LOAD")){
                            code.add(tempList.get(tempList.size() - 1));
                            tempList.remove(tempList.get(tempList.size() - 1));
                        }
                        code.add(tempList.get(tempList.size() - 1));
                        tempList.remove(tempList.get(tempList.size() - 1));
//                        code.add(new IntermediateCode("LABEL", elseLabel, null, null));
                        valueStack.pop();
                        code.add(new IntermediateCode("LABEL", endLabel, null, null));
                        code.addAll(tempList);
                    }else{
                        endLabel = "L" + labelCount++;
                        boolVal = valueStack.pop();
                        code.add(new IntermediateCode("LABEL", endLabel, null, null));
                        tempList = new ArrayList<>();
                        // 条件跳转指令
                        while(!intermediateCode.isEmpty() && (intermediateCode.get(intermediateCode.size() - 1).getResult() == null || !intermediateCode.get(intermediateCode.size() - 1).getResult().equals(boolVal))){
                            tempList.add(intermediateCode.get(intermediateCode.size() - 1));
                            intermediateCode.remove(intermediateCode.get(intermediateCode.size() - 1));
                        }
                        code.addAll(tempList);
                    }

                    break;
                // stmt -> while (bool) stmt
                case 12:
                    // 值栈内容 [while,(,bool,),stmt]
                    // 弹出 } 或者 {
                    if (valueStack.peek().equals("}")) valueStack.pop();
                    if (valueStack.peek().equals("{")) valueStack.pop();

                    int loopStartIndex = intermediateCode.size(); // 记录循环体开始位置

                    if (valueStack.peek().equals(")")) valueStack.pop();
                    String whileBool = valueStack.pop();// bool表达式的值
                    if (valueStack.peek().equals("(")) valueStack.pop();
                    valueStack.pop();// while关键字

                    // 生成标签
                    String startLabel = "L" + labelCount++;  // 循环开始标签
                    String whileEndLabel = "L" + labelCount++;    // 循环结束标签

                    breakLabelStack.push(whileEndLabel);

//                    // 临时列表
//                    ArrayList<IntermediateCode> whileCode = new ArrayList<>();
//
//                    whileCode.add(new IntermediateCode("LABEL", startLabel, null, null));
//                    whileCode.add(new IntermediateCode("IF_FALSE", whileBool, "GOTO " + whileEndLabel, null));
//
//                    code.add(new IntermediateCode("GOTO", startLabel, null, null));// 循环开始
//                    code.add(new IntermediateCode("LABEL", whileEndLabel, null, null));// 循环结束
//
//                    code.addAll(whileCode);
//                    // valueStack.push(whileEndLabel);
//                    breakLabelStack.pop();
                    // 插入循环入口标签和条件跳转
                    intermediateCode.add(loopStartIndex, new IntermediateCode("LABEL", startLabel, null, null));
                    intermediateCode.add(loopStartIndex + 1, new IntermediateCode("IF_FALSE", whileBool, "GOTO " + whileEndLabel, null));

                    // 插入循环末尾跳转回开始
                    intermediateCode.add(new IntermediateCode("GOTO", startLabel, null, null));
                    intermediateCode.add(new IntermediateCode("LABEL", whileEndLabel, null, null));

                    breakLabelStack.pop();
                    breakLabelStack.push(whileEndLabel); // 用于 break;

                    break;
                // stmt -> break;
                case 14:
                    // 值栈内容 [break,;]
                    valueStack.pop();
                    valueStack.pop();
                    if (breakLabelStack.isEmpty()) {
                        // 如果没有对应的循环标签，break语句非法
                        SemanticErrors.add(generateSemanticError("break语句不在循环或switch中使用", line, position));
                        break;
                    }
                    // 获取当前最近的循环结束标签
                    String breakLabel = breakLabelStack.peek();
                    // 生成跳转到结束标签的中间代码
                    code.add(new IntermediateCode("GOTO", breakLabel, null, null));

                    break;
                // loc → loc [ bool ]
                case 16:
                    valueStack.pop(); // ]
                    String index = valueStack.pop();
                    valueStack.pop(); // [
                    String array = valueStack.pop();
                    String arrayTemp = "t" + tempVarCount++;
                    code.add(new IntermediateCode("ARRAY_ADDR", array, index, arrayTemp));
                    valueStack.push(arrayTemp);
                    arrayAddrOrigin.put(arrayTemp, array); // 记录 tX -> arr
                    break;
                // loc -> id
                case 17:
                    // 从值栈中获取id
                    String idValue = valueStack.pop();
                    if (!symbolTable.contains(idValue)) {
                        SemanticErrors.add(generateSemanticError("变量" + idValue + "未声明", line, position));
                    }
                    // 将id压回值栈，供上层使用
                    valueStack.push(idValue);
                    break;
                // 布尔运算优化
                case 18: // bool → bool || join
                    // 弹出右操作数（join的值）
                    if(valueStack.peek().equals(")"))valueStack.pop();
                    String rightOperand = valueStack.pop();
                    if(valueStack.peek().equals("(")) valueStack.pop();
                    // 弹出逻辑或符号（需要确保符号栈已处理||符号）
                    valueStack.pop(); // 弹出||
                    // 弹出左操作数（之前bool的值）
                    if(valueStack.peek().equals(")"))valueStack.pop();
                    String leftOperand = valueStack.pop();
                    if(valueStack.peek().equals("(")) valueStack.pop();

                    // 生成短路逻辑
                    String temp = "t" + tempVarCount++;
                    trueLabel = "L" + labelCount++;
                    endLabel = "L" + labelCount++;
                    // 关键修改：提取右操作数的中间代码
//                    List<IntermediateCode>tempList1 = new ArrayList<>();
//                    List<IntermediateCode>tempList2 = new ArrayList<>();
//                    // -> (true)
//                    while (!intermediateCode.isEmpty() && (
//                            !intermediateCode.get(intermediateCode.size()-1).getResult().equals(leftOperand)
//                            )) {
//                        tempList1.add(intermediateCode.remove(intermediateCode.size()-1));
//                    }
//                    //
//                    while (!intermediateCode.isEmpty() && (
//                            intermediateCode.get(intermediateCode.size()-1).getResult()==null||
//                            !intermediateCode.get(intermediateCode.size()-1).getResult().equals("LOAD")
//                    )) {
//                        tempList2.add(intermediateCode.remove(intermediateCode.size()-1));
//                    }

//                    Collections.reverse(tempList1); // 恢复原顺序
//                    Collections.reverse(tempList2);
                    // 生成左操作数判断逻辑
                    code.add(new IntermediateCode("IF_TRUE", leftOperand, "GOTO " + trueLabel, null));
                    code.add(new IntermediateCode("IF_TRUE", rightOperand, "GOTO " + trueLabel, null));
                    code.add(new IntermediateCode("IF_FALSE", temp, "GOTO " + endLabel, null));
                    // 生成标签和真值赋值
                    code.add(new IntermediateCode("LABEL", trueLabel, null, null));
                    code.add(new IntermediateCode("LABEL", endLabel, null, null));

                    valueStack.push(temp);
                    break;
                case 19: // bool → join
                    // 直接将join的值压入栈中
                    String joinValue = valueStack.pop();
                    valueStack.push(joinValue);
                    break;
                // join -> join && equality
                case 20:
                    // 值栈内容：[join,&&,equality]
                    String equalityResult=valueStack.pop();
                    valueStack.pop();//弹出&&
                    String joinLeft=valueStack.pop();
                    String tempVarAnd="t"+tempVarCount++;
                    String labelTrueAnd="L"+labelCount++;
                    String labelFalseAnd="L"+labelCount++;
                    String labelEndAnd="L"+labelCount++;

                    //生成中间代码
                    code.add(new IntermediateCode("IFNOT",joinLeft,null,labelFalseAnd));
                    code.add(new IntermediateCode("ASSIGN",equalityResult,null,tempVarAnd));
                    code.add(new IntermediateCode("GOTO",null,null,labelEndAnd));
                    code.add(new IntermediateCode("LABEL",null,null,labelFalseAnd));
                    code.add(new IntermediateCode("ASSIGN","false",null,tempVarAnd));
                    code.add(new IntermediateCode("LABEL",null,null,labelEndAnd));

                    valueStack.push(tempVarAnd);
                    break;
                // join -> equality
                case 21:
                    // 值栈内容：[equality]
                    String equalityValue=valueStack.pop();
                    valueStack.push(equalityValue);
                    break;


                // 比较运算符统一处理
                case 22: //==
                case 23: //!=
                case 25: // <
                case 26: // <=
                case 27: // >=
                case 28: // >
                    String right = valueStack.pop();
                    valueStack.pop();//弹出符号
                    String left = valueStack.pop();
                    String compOp = getRelOp(id); // 根据产生式ID获取运算符
                    String compTemp = "t" + tempVarCount++;
                    String compLabel = "t" + tempVarCount++;
                    code.add(new IntermediateCode("CMP", left, right, compTemp));
                    code.add(new IntermediateCode(compOp, compTemp, "0", compLabel));
                    valueStack.pop();
                    valueStack.push(compLabel);

                    break;
                // expr → expr + term
                case 30:
                    String term = valueStack.pop();
                    valueStack.pop(); // 弹出+
                    String expr = valueStack.pop();
                    result = "t" + tempVarCount++;
                    code.add(new IntermediateCode("ADD", expr, term, result));
                    valueStack.push(result); // 结果压栈供上层使用
                    break;
                // expr → expr - term
                case 31:
                    String subTerm = valueStack.pop();
                    valueStack.pop(); // 弹出+
                    String subExpr = valueStack.pop();
                    result = "t" + tempVarCount++;
                    code.add(new IntermediateCode("SUB", subExpr, subTerm, result));
                    valueStack.push(result);
                    break;
                // term → term * unary
                case 33:
                    String unary1 = valueStack.pop(); // unary
                    valueStack.pop(); // 弹出*
                    String term1 = valueStack.pop(); // term
                    result = "t" + tempVarCount++;
                    code.add(new IntermediateCode("MUL", term1, unary1,result));
                    valueStack.push(result); // 结果压栈供上层使用
                    break;
                // term → term / unary
                case 34:
                    unary1 = valueStack.pop(); // unary
                    term1 = valueStack.pop(); // term
                    result = "t" + tempVarCount++;
                    code.add(new IntermediateCode("/", term1, unary1,result));
                    valueStack.push(result);
                    break;
                // unary → -unary
                case 37:
                    String unary = valueStack.pop(); // unary
                    result = "t" + tempVarCount++;
                    code.add(new IntermediateCode("NEG", unary,null,result));
                    valueStack.push(result);
                    break;
                case 39: // factor → ( bool )
                    valueStack.pop(); // 弹出右括号
//                    boolVal = valueStack.pop();
                    if(valueStack.peek().equals("("))valueStack.pop(); // 弹出左括号
//                    result = "t" + tempVarCount++;
//                    valueStack.push(result);  // 仅保留布尔值
                    break;
                // factor → loc
                case 40:
                    String location = valueStack.pop(); // loc
                    result = "t" + tempVarCount++;
                    code.add(new IntermediateCode("LOAD", location,null,result));
                    valueStack.push(result);
                    break;
                // factor → num
                case 41:
                    String number = valueStack.pop(); // num
                    result = "t" + tempVarCount++;
                    numTable.insert(result,number,getType(number));
                    code.add(new IntermediateCode("CONST", number,null,result));
                    valueStack.push(result);
                    break;


                default:
                    // 添加默认处理
                    System.out.println("暂未实现的产生式ID: " + id);
                    break;
            }
            return code;
        }

        // 辅助方法：获取比较运算符
        private String getRelOp(int prodId) {
            switch(prodId) {
                case 22: return "JE";
                case 23: return "JNE";
                case 25: return "LT";
                case 26: return "LE";
                case 27: return "GE";
                case 28: return "GT";
                default: return "CMP";
            }
        }

        // 辅助方法：获取语义错误
        private String generateSemanticError(String msg, int line, int position) {
            return new String(msg + " (行号:" + line + ",列号:" + position + ")");
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
    // 定义运算符优先级（数值越大优先级越高）
    static Map<String, Integer> precedence = new HashMap<>();
    static {
        precedence.put("if", 10);
        precedence.put("else", 10);
        precedence.put("||", 20);
        precedence.put("&&", 30);
        precedence.put("==", 40);
        precedence.put("!=", 40);
        precedence.put("<", 50);
        precedence.put(">", 50);
        precedence.put("+", 60);
        precedence.put("-", 60);
        precedence.put("*", 70);
        precedence.put("/", 70);
        precedence.put("!", 80);
        precedence.put("=", 90);  // 赋值操作符优先级最低
    }
    // 定义结合性（LEFT/RIGHT/NONE）
    static Map<String, String> associativity = new HashMap<>();
    static {
        associativity.put("+", "LEFT");
        associativity.put("-", "LEFT");
        associativity.put("*", "LEFT");
        associativity.put("/", "LEFT");
        associativity.put("=", "RIGHT");
        associativity.put("&&", "LEFT");
        associativity.put("||", "LEFT");
    }

    static List<String> terminals = Arrays.asList(
            "{", "}", ";", "id", "num", "true", "false", "(", ")", "real",
            "||", "&&", "==", "!=", "<", "<=", ">", ">=", "+", "-", "*", "/",
            "!", "=", "if", "else", "while", "do", "break", "int", "float", "boolean","[","]","$"

    );
    static List<String> nonTerminals = Arrays.asList(
            "program", "block", "decls", "decl", "type", "stmts", "stmt",
            "loc", "bool", "join", "equality", "rel", "expr", "term", "unary", "factor","basic"
    );

    public static void main(String[] args) throws Exception {


        String input = "{ \n" +
                "  int i; \n" +
                "  i = 0; \n" +
                "  if(i < 1) { \n" +
                "    i = i + 1; \n" +
                "  } \n" +
                "}\n";


        initializeProductions();
        computeFirstSets();
        buildParser();
//        printAnalysisTables();
//        exportToExcel();
//        printTables();
        EnhancedLexer analysis = new EnhancedLexer();
        List<Token> tokens = analysis.analyze(input);
        List<ParseStep> steps = parse(tokens);
        for (ParseStep step : steps) {
            System.out.println("状态栈: " + step.states + ", 符号栈: " + step.symbols + ", 输入串: " + step.input + ", 动作: " + step.action);
        }

        System.out.println("生成的中间代码：");
        for (IntermediateCode code : intermediateCode) {
            System.out.println(code);
        }

        System.out.println("语义分析错误：");
        for (String error: SemanticErrors) {
            System.out.println(error);
        }
    }

    /**
     * 确定Token对应的符号
     * @param token
     * @return
     */
    public static String determineSymbol(Token token) {
        if (token.value == null) {
            throw new IllegalArgumentException("Token value cannot be null");
        }
        if (token.type == TokenType.IDENTIFIER) {
            return "id";
        } else if (token.type == TokenType.NUMERIC_CONST) {
            return "num";
        }
        return token.value;
    }

    // 临时变量计数器
    static int tempCount = 0;
    static int labelCount = 0;  // 标签计数器
    // 操作数栈
    static Stack<String> operandStack = new Stack<>();

    // 生成临时变量
    private static String newTemp() {
        return "t" + (tempCount++);
    }
    public static List<ParseStep> parse(List<Token> tokens) {
        // 初始化状态栈、符号栈和输入符号流
        symbolTable.clear();
        intermediateCode.clear();
        SemanticErrors.clear();
        Stack<Integer> stateStack = new Stack<>();
        Stack<String> symbolStack = new Stack<>();
        List<String> inputSymbols = new ArrayList<>();
        List<Token> inputTokens = new ArrayList<>();
        List<ParseStep> parseSteps = new ArrayList<>();
        //Map<String, String> symbolTable = new HashMap<>(); // 变量名 → 类型

        // 为语义分析错误定位
        int line = 0, position = 0;

        // 引入语义栈 与 符号栈 解耦
        //Stack<String> valueStack = new Stack<>();
        valueStack.push("$");

        stateStack.push(0);
        symbolStack.push("$");

        // 初始化输入符号流
        for (Token token : tokens) {
            inputSymbols.add(determineSymbol(token));
            inputTokens.add(token);  // 保存 token 本身，便于出错时定位
        }
        inputSymbols.add("$");

        // 给 tokens 也加个 $
        inputTokens.add(new Token(TokenType.END, "$", -1, -1));  // 行列号用 -1 表示结束符

        while (true) {
            int currentState = stateStack.peek();
            String currentSymbol = inputSymbols.get(0);
//            System.out.println("当前状态: " + currentState + ", 当前符号: " + currentSymbol);

            Map<String, String> actionRow = actionTable.get(currentState);
            if (actionRow == null || !actionRow.containsKey(currentSymbol)) {
                Token errorToken = inputTokens.get(0);

                // 改进的错误输出（带行列号）
                String errorDetail;
                if (!currentSymbol.equals("$")) {
                    errorDetail = String.format(
                            "行号: %d, 列号: %d, 错误符号: '%s'",
                            errorToken.getLine(),
                            errorToken.getPosition(),
                            errorToken.value
                    );
                } else {
                    errorDetail = "错误位置: 文件末尾";
                }

                System.err.println("==============================");
                System.err.println("语法分析错误: " + errorDetail);
                System.err.println("当前状态: " + currentState);
                System.err.println("期待符号: " + actionRow.keySet());
                System.err.println("==============================");

                throw new LR1ParserException(
                        String.format("无法处理符号 '%s' (%s)", currentSymbol, errorDetail),
                        parseSteps
                );
            }
            String rawAction = actionRow.get(currentSymbol);

            String actionDescription;

            // 文字描述
            if (rawAction.startsWith("s")) {
                actionDescription = "移入 " + currentSymbol;
            } else if (rawAction.startsWith("r")) {
                int prodId = Integer.parseInt(rawAction.substring(1));
                Production prod = productions.get(prodId);
                actionDescription = "规约 " + prod.lhs + " -> " + String.join(" ", prod.rhs);
            } else if (rawAction.equals("acc")) {
                actionDescription = "接受";
            } else {
                throw new LR1ParserException("未知动作: " + rawAction, parseSteps);
            }

            parseSteps.add(new ParseStep(
                    new ArrayList<>(stateStack),
                    new ArrayList<>(symbolStack),
                    new ArrayList<>(inputSymbols),
                    actionDescription
            ));
            System.out.println("动作: " + actionDescription);
            System.out.println("状态栈: " + stateStack);
            System.out.println("符号栈: " + symbolStack);
            System.out.println("输入串: " + inputSymbols);
            System.out.println("==============================================");
            if (rawAction.startsWith("s")) {
                int nextState = Integer.parseInt(rawAction.substring(1));
                symbolStack.push(currentSymbol);
                valueStack.push(inputTokens.get(0).value);//使用Token的值作为语义值
                line = inputTokens.get(0).line;
                position = inputTokens.get(0).position;
                stateStack.push(nextState);
                inputSymbols.remove(0);
                inputTokens.remove(0);
            }  else if (rawAction.startsWith("r")) {
                int prodId = Integer.parseInt(rawAction.substring(1));
                Production prod = productions.get(prodId);
                for (int i = 0; i < prod.rhs.length; i++) {
                    symbolStack.pop();
                    stateStack.pop();

//                    if (operandStack.size() > 0) {
//                        operandStack.pop();
//                        operandStack.push(prod.lhs);
//
//                    }
                }
                symbolStack.push(prod.lhs);
                int topState = stateStack.peek();
                int gotoState = gotoTable.get(topState).get(prod.lhs);
                stateStack.push(gotoState);


                // 生成中间代码
                List<IntermediateCode> generatedCode = prod.generateCode(valueStack,symbolStack, line, position);
                intermediateCode.addAll(generatedCode);
                // 打印中间代码
                System.out.println("生成的中间代码：");
                for (IntermediateCode code : generatedCode) {
                    System.out.println(code);
                }

//                // 将结果压入值栈
//                if (result != null) {
//                    valueStack.push(result);
//                }
                // 语义规则处理
//                applySemanticRules(prodId);
            }
            else if (rawAction.equals("acc")) {
                System.out.println("分析成功！");
                break;
            }
        }
        return parseSteps;
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
        productions.add(new Production("stmt", new String[]{"if","(", "bool", ")",  "stmt", "else", "stmt"}, 11)); // if-else
        productions.add(new Production("stmt", new String[]{"while", "(", "bool", ")", "stmt"}, 12)); // while(bool)stmt
        productions.add(new Production("stmt", new String[]{"do", "stmt", "while", "(", "bool", ")", ";"}, 13)); // do-while
        productions.add(new Production("stmt", new String[]{"break", ";"}, 14)); // break;
        productions.add(new Production("stmt", new String[]{"block"}, 15)); // block

        // 左值
        productions.add(new Production("loc", new String[]{"loc", "[", "bool", "]"}, 16));
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
        productions.add(new Production("basic", new String[]{"boolean"}, 47));
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
                    System.out.println("报告：动态处理未识别的符号："+p.lhs);
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

    // 计算产生式右侧从某一位置开始的FIRST集，处理ε情况
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

            // 处理归约项（前置，优先处理接受动作）
            for (LR1Item item : currentState) {
                if (item.dot == item.prod.rhs.length) {
                    // 处理接受动作（特殊产生式 S' -> S）
                    if (item.prod == productions.get(0)) {
                        actionTable.computeIfAbsent(stateId, k -> new HashMap<>())
                                .put("$", "acc");
                        continue;
                    }

                    for (String la : item.lookaheads) {
                        String action = "r" + item.prod.id;

                        // 处理else冲突（强制移进）
                        if (la.equals("else")) {
                            Integer shiftState = getShiftState(stateId, "else");
                            if (shiftState != null) {
                                action = "s" + shiftState;
                                System.out.println("应用else优先级: 状态"+stateId+" 符号else强制移进到"+shiftState);
                                actionTable.computeIfAbsent(stateId, k -> new HashMap<>())
                                        .put(la, action);
                                continue; // 跳过后续冲突处理
                            }
                        }

                        // 冲突检测与处理
                        if (actionTable.containsKey(stateId) &&
                                actionTable.get(stateId).containsKey(la)) {

                            String existingAction = actionTable.get(stateId).get(la);
                            boolean isConflict = !existingAction.equals(action);

                            if (isConflict) {
                                // 获取优先级（区分移进/归约）
                                int existingPrec = getPrecedence(existingAction);
//                                        existingAction.startsWith("s")
//                                        ? precedence.getOrDefault(la, -1)  // 移行动作取符号优先级
//                                        : productions.get(Integer.parseInt(existingAction.substring(1))).prec;
                                int newPrec = getPrecedence(action);
//                                int newPrec = precedence.getOrDefault(item.prod.id, -1) ; // 归约动作取产生式优先级

                                // 优先级比较
                                if (newPrec > existingPrec) {
                                    actionTable.get(stateId).put(la, action);
                                    System.out.println("解决冲突: 状态"+stateId+" 符号"+la+" 新动作 "+action);
                                }
                                else if (newPrec == existingPrec) {
                                    // 结合性处理
                                    String assoc = associativity.getOrDefault(la, "NONE");
                                    if (assoc.equals("LEFT")) {
                                        actionTable.get(stateId).put(la, action);
                                        System.out.println("结合性解决: 状态"+stateId+" 符号"+la+" 左结合选归约");
                                    }
                                    else if (assoc.equals("RIGHT") && existingAction.startsWith("s")) {
                                        System.out.println("结合性解决: 状态"+stateId+" 符号"+la+" 右结合保留移进");
                                    }
                                    else {
                                        System.err.println("未解决冲突: 状态"+stateId+" 符号"+la+
                                                " 原有:"+existingAction+" 新:"+action);
                                    }
                                }
                                else {
                                    System.err.println("未解决冲突: 状态"+stateId+" 符号"+la+
                                            " 原有动作优先级更高");
                                }
                            }
                        }
                        else {
                            // 无冲突直接插入
                            actionTable.computeIfAbsent(stateId, k -> new HashMap<>())
                                    .put(la, action);
                        }
                    }
                }
            }

            // 合并所有可能的符号（后置，避免状态扩展干扰）
            Set<String> allSymbols = new LinkedHashSet<>(terminals);
            allSymbols.addAll(nonTerminals);

            // 构建状态转移
            for (String symbol : allSymbols) {
                Set<LR1Item> newState = goTo(currentState, symbol);
                if (!newState.isEmpty()) {
                    int existing = findState(newState);
                    if (existing == -1) {
                        states.add(newState);
                        existing = states.size() - 1;
                        queue.add(existing);
                    }

                    // 更新分析表
                    if (terminals.contains(symbol)) {
                        actionTable.computeIfAbsent(stateId, k -> new HashMap<>())
                                .put(symbol, "s" + existing);
                    } else {
                        gotoTable.computeIfAbsent(stateId, k -> new HashMap<>())
                                .put(symbol, existing);
                    }
                }
            }
        }
    }
    // 获取动作的优先级
    private static int getPrecedence(String action) {
        if (action.startsWith("s")) {
            // 移进动作的优先级由当前符号决定
            String symbol = getSymbolFromAction(action);
            return precedence.getOrDefault(symbol, 0);
        } else if (action.startsWith("r")) {
            // 归约动作的优先级由产生式左侧符号决定
            int prodId = Integer.parseInt(action.substring(1));
            Production prod = productions.get(prodId);
            return precedence.getOrDefault(prod.lhs, 0);
        }
        return 0;
    }
    // 从动作中提取移进的符号
    private static String getSymbolFromAction(String action) {
        if (action.startsWith("s")) {
            int index = Integer.parseInt(action.substring(1));
            // 添加索引范围检查
            if (index >= 0 && index < terminals.size()) {
                return terminals.get(index);
            }
            return "UNKNOWN_SYMBOL"; // 或抛出更明确的异常
        }
        return "";
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
    static void printTables() {
        System.out.println("\n============ 表 ============");
        // 打印表头
        System.out.printf("%-6s", "State");
        for (String t : terminals) {
            System.out.printf("%-8s", t);
        }
        for (String nt : nonTerminals) {
            System.out.printf("%-8s", nt);
        }
        System.out.println();

        // 打印内容
        for (int state = 0; state < states.size(); state++) {
            System.out.printf("%-6d", state);
            for (String t : terminals) {
                String action = actionTable.getOrDefault(state, Collections.emptyMap()).get(t);
                System.out.printf("%-8s", action != null ? action : "");
            }
            for (String nt : nonTerminals) {
                Integer gotoState = gotoTable.getOrDefault(state, Collections.emptyMap()).get(nt);
                System.out.printf("%-8s", gotoState != null ? gotoState : "");
            }
            System.out.println();
        }
    }
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

    // 定义临时变量存储
    private static class NumInfo{
        public String name;
        public String value;
        public String type;

        public NumInfo(String name, String value, String type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }
    }

    // 定义数据表类
    private static class NumTable{
        private Map<String, NumInfo> table;

        public NumTable() {
            table = new HashMap<>();
        }

        public void insert(String name,String value,String type){
            if ( type.equals("double")){
                type = "float";
            }
            table.put(name, new NumInfo(name,value,type));
        }

        public NumInfo getNumInfo(String name){
            return table.get(name);
        }
    }

    // 定义符号信息类
    private static class SymbolInfo {
        public String name;
        public String type;

        public SymbolInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    // 定义符号表类 用于存储已声明的变量名及变量类型
     private static class SymbolTable {
        private Map<String, SymbolInfo> table;

        public SymbolTable() {
            table = new HashMap<>();
        }

        // 插入一个新的
        public void insert(String name, String type) {

            table.put(name, new SymbolInfo(name, type));
        }

        // 查找
        public String lookupType(String name) {
            return table.get(name).type;
        }
        // 是否包含这个变量名
        public boolean contains(String name) {
            return table.containsKey(name);
        }

        // 清空
        public void clear(){
            table.clear();
        }
    }

    // 根据传入的值返回其数据类型
    public static String getType(String value) {
        // 检查布尔类型
        if ("true".equals(value) || "false".equals(value)) {
            return "bool";
        }

        // 检查整数类型
        if (value.matches("-?\\d+")) {
            return "int";
        }

        // 检查浮点数类型
        if (value.matches("-?\\d+\\.\\d+")) {
            return "double";
        }

        // 默认为字符串类型
        return "string";
    }

    // 转换值类型
    public static String convertType(String value, String targetType) {
        switch (targetType) {
            case "int":
                // 将值转换为整数
                return String.valueOf((int) Double.parseDouble(value));
            case "float":
                // 将值转换为浮点数
                return String.valueOf(Float.parseFloat(value));
            case "double":
                // 将值转换为双精度浮点数
                return String.valueOf(Double.parseDouble(value));
            default:
                throw new IllegalArgumentException("Unsupported target type: " + targetType);
        }
    }
}