import java.util.List;

/**
 * @Author: Lx
 * @Package: PACKAGE_NAME
 * @Project: FullLR1Parser.java
 * @name: LR1ParserException
 * @Date: 2025/4/28 9:32
 * @Filename: LR1ParserException
 */
public class LR1ParserException extends RuntimeException{
    // 记录已有的步骤
    private List<ParseStep> steps;

    public LR1ParserException(String message,List<ParseStep> steps) {
        super(message);
        this.steps = steps;
    }

    public List<ParseStep> getSteps(){
        return steps;
    }
}
