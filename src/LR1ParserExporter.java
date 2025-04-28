import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LR1ParserExporter {

    // 导出ACTION表和GOTO表到同一个CSV文件
    public static void exportTablesToCSV(String filename) {
        try (FileWriter csvWriter = new FileWriter(filename)) {
            // 写ACTION表的表头
            csvWriter.append("State");
            for (String terminal : FullLR1Parser.terminals) {
                csvWriter.append(",").append(terminal);
            }
            // 写GOTO表的表头
            for (String nonTerminal : FullLR1Parser.nonTerminals) {
                csvWriter.append(",").append(nonTerminal);
            }
            csvWriter.append("\n");

            // 写内容
            for (int state = 0; state < FullLR1Parser.states.size(); state++) {
                csvWriter.append(String.valueOf(state));
                // 写ACTION表的内容
                for (String terminal : FullLR1Parser.terminals) {
                    String action = FullLR1Parser.actionTable.getOrDefault(state, Collections.emptyMap()).getOrDefault(terminal, "");
                    csvWriter.append(",").append(action);
                }
                // 写GOTO表的内容
                for (String nonTerminal : FullLR1Parser.nonTerminals) {
                    Integer gotoState = FullLR1Parser.gotoTable.getOrDefault(state, Collections.emptyMap()).getOrDefault(nonTerminal, -1);
                    csvWriter.append(",").append(String.valueOf(gotoState));
                }
                csvWriter.append("\n");
            }

            System.out.println("分析表已导出到 " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 构建分析器
        FullLR1Parser.initializeProductions();
        FullLR1Parser.computeFirstSets();
        FullLR1Parser.buildParser();

        // 导出分析表
        exportTablesToCSV("lr1_tables.csv");
        System.out.println("导出完成！");
    }
}