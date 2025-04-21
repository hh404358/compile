import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @Author: hsy
 * @BelongsProject: 代码
 * @BelongsPackage: PACKAGE_NAME
 * @CreateTime: 2025-04-21  15:40
 * @Name:Word
 * @Description: TODO
 * @Version: 1.0
 */

public class WordForm {
    private JTabbedPane tabbedPane;
    private JPanel wordPanel;
    private JPanel syntaxPanel;
    private JButton wordButton;
    private JTextArea scanTextArea;
    private JTextArea errorTextArea;
    private JTextArea partitionTextArea;
    private JTextArea signTableTextArea;
    private JTextArea inputTextArea;
    private JPanel root;
    private JPanel left;
    private JPanel mid;
    private JPanel right;
    private JPanel error;
    private JPanel rightUp;
    private JPanel partition;
    private JPanel signTable;
    private JLabel label1;
    private JLabel label2;
    private JLabel label4;
    private JLabel label5;
    private JLabel label3;
    private JScrollPane scrollPane2;
    private JScrollPane scrollPane3;
    private JScrollPane scrollPane4;
    private JScrollPane scrollPane5;
    private JScrollPane scrollPane1;

    public WordForm() {
        tabbedPane.setTitleAt(0, "词法分析");
        tabbedPane.setTitleAt(1, "语法分析");
        // 点击“词法分析”按钮
        wordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AdvancedLexer analysis = new AdvancedLexer();//创建词法分析对象
                String input = inputTextArea.getText(); // 从 inputTextArea 读取文本
                 String scan_result = analysis.preprocess(input); //获取删除注释和空格后的代码
                 analysis.analyze(input); // 调用词法分析方法
                 String partition_result = analysis.getTokenList(); // 获取分割单词的结果
                 String sign_table_result = analysis.getSymbolTable(); // 获取符号表
                 String error_result = analysis.getErrorList(); //获取错误列表
                 scanTextArea.setText(scan_result);
                 errorTextArea.setText(error_result);
                 partitionTextArea.setText(partition_result);
                 signTableTextArea.setText(sign_table_result);
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("编译原理实践");
        frame.setContentPane(new WordForm().root);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setSize(1200, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
