import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * @Author:zyt
 * @Package:PACKAGE_NAME
 * @Project:FullLR1Parser.java
 * @Name:IntermediateCode
 * @Date:2025/4/29 19:20
 * @Filename:IntermediateCode
 */

public class IntermediateCode {
    private final String operator;    // 操作符（如 "ADD", "ASSIGN"）
    //private final List<String> operands; // 操作数列表（动态长度）
    //static int tempVarCount=0;
    private String arg1;
    private String arg2;
    private String result;

    public IntermediateCode(String operator,String arg1,String arg2,String result) {
        this.operator = operator;
        //this.operands = Arrays.asList(operands);
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }

    @Override
    public String toString() {
        // 通用格式化：操作符 + 空格分隔的操作数
        return operator + " " +arg1+" "+arg2+" "+result;
    }

}

//// 语法制导翻译技术 定义用于表示中间代码的类 三地址表示法
//class IntermediateCode {
//    String op; // 操作符（+, -, *, /, =, if, goto 等）
//    String arg1; // 第一个操作数
//    String arg2; // 第二个操作数（如果有）
//    String result; // 结果（临时变量或标签）
//    static int tempVarCount = 0; // 临时变量计数器
//
//    public IntermediateCode(String op, String arg1, String arg2, String result) {
//        this.op = op;
//        this.arg1 = arg1;
//        this.arg2 = arg2;
//        this.result = result;
//    }
//
//    @Override
//    public String toString() {
//        if (op.equals("if")) {
//            return String.format("if %s %s %s goto %s", arg1, op, arg2, result);
//        } else if (op.equals("goto")) {
//            return String.format("goto %s", result);
//        } else if (arg2 == null) {
//            return String.format("%s = %s", result, arg1);
//        } else {
//            return String.format("%s = %s %s %s", result, arg1, op, arg2);
//        }
//    }
//}
