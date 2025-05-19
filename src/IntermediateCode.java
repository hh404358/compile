import java.util.*;

/**
 * @Author:zyt
 * @Package:PACKAGE_NAME
 * @Project:FullLR1Parser.java
 * @Name:IntermediateCode
 * @Date:2025/4/29 19:20
 * @Filename:IntermediateCode
 */

public class IntermediateCode {
    // 操作符（如 "ADD", "ASSIGN"）
    private final String operator;
    private String arg1;
    private String arg2;
    private String result;

    public IntermediateCode(String operator, String arg1, String arg2, String result) {
        this.operator = operator;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }
    public IntermediateCode(String operator, String arg1, String arg2, String result,boolean isPlaceholer) {
        this.operator = operator;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }

    public String getOperator() {
        return operator;
    }

    public String getArg1() {
        return arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public String getResult() {
        return result;
    }

    @Override
    public String toString() {
        // 通用格式化：操作符 + 空格分隔的操作数
        return operator + " " + arg1 + " " + arg2 + " " + result;
    }
}
