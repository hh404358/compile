import javax.swing.*;
import java.awt.*;
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
    private JPanel midcodePanel;


    public WordForm() {
        tabbedPane.setTitleAt(0, "词法分析");
        tabbedPane.setTitleAt(1, "语法分析");
        // 1. 设置全局字体和样式
        Font font = new Font("Microsoft YaHei", Font.PLAIN, 14);
        Font boldFont = new Font("Microsoft YaHei", Font.BOLD, 14);
        Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 16);

        // 2. 设置TabbedPane样式
        tabbedPane.setFont(titleFont);
        tabbedPane.setBackground(new Color(240, 240, 240));
        tabbedPane.setForeground(new Color(70, 130, 180));

        // 3. 设置按钮样式
        wordButton.setFont(boldFont);
        wordButton.setBackground(new Color(70, 130, 180));
        wordButton.setForeground(Color.WHITE);
        wordButton.setFocusPainted(false);
        wordButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        wordButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 4. 设置标签样式
        label1.setFont(boldFont);
        label2.setFont(boldFont);
        label3.setFont(boldFont);
        label4.setFont(boldFont);
        label5.setFont(boldFont);

        // 5. 设置面板背景色
        root.setBackground(new Color(240, 240, 240));
        left.setBackground(new Color(240, 240, 240));
        mid.setBackground(new Color(240, 240, 240));
        right.setBackground(new Color(240, 240, 240));
        rightUp.setBackground(new Color(240, 240, 240));

        // 6. 设置间距
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        left.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        right.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 点击“词法分析”按钮
        wordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                AdvancedLexer analysis = new AdvancedLexer();//创建词法分析对象
//                String input = inputTextArea.getText(); // 从 inputTextArea 读取文本
//                 String scan_result = analysis.preprocess(input); //获取删除注释和空格后的代码
//                 analysis.wordAnalyze(input); // 调用词法分析方法
//                 String partition_result = analysis.getTokenList(); // 获取分割单词的结果
//                 String sign_table_result = analysis.getSymbolTable(); // 获取符号表
//                 String error_result = analysis.getErrorList(); //获取错误列表
                EnhancedLexer analysis = new EnhancedLexer();//创建词法分析对象
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
