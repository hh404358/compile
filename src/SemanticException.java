import java.util.List;

/**
 * @Author: hsy
 * @BelongsProject: FullLR1Parser.java
 * @BelongsPackage: PACKAGE_NAME
 * @CreateTime: 2025-05-12  16:02
 * @Name:SemanticException
 * @Description: TODO
 * @Version: 1.0
 */
public class SemanticException extends RuntimeException{
    // 记录已有的步骤
    private List<IntermediateCode> intermediateCode;

    public SemanticException(String message,List<IntermediateCode> intermediateCode) {
        super(message);
        this.intermediateCode = intermediateCode;
    }

    public List<IntermediateCode> getCodes(){
        return intermediateCode;
    }
}
