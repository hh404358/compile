import javax.swing.*;
import javax.swing.border.TitledBorder;
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
    private JPanel tablePanel;
    private JTable table1;


    public WordForm() {
            tabbedPane.setTitleAt(0, "词法分析");
            tabbedPane.setTitleAt(1, "语法分析");

            // 初始化语法分析面板
            initSyntaxPanel();

            // 统一设置全局样式
            setGlobalStyles();

            // 设置词法分析按钮事件
            wordButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    EnhancedLexer analysis = new EnhancedLexer();
                    String input = inputTextArea.getText();
                    String scan_result = analysis.pre(input);
                    analysis.analyze(input);
                    String partition_result = analysis.getTokenList();
                    String sign_table_result = analysis.getSymbolTable();
                    String error_result = analysis.getErrorList();

                    scanTextArea.setText(scan_result);
                    errorTextArea.setText(error_result);
                    partitionTextArea.setText(partition_result);
                    signTableTextArea.setText(sign_table_result);
                }
            });
    }

        private void setGlobalStyles() {
            Font font = new Font("Microsoft YaHei", Font.PLAIN, 14);
            Font boldFont = new Font("Microsoft YaHei", Font.BOLD, 14);
            Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 16);

            // 设置TabbedPane样式
            tabbedPane.setFont(titleFont);
            tabbedPane.setBackground(new Color(240, 240, 240));
            tabbedPane.setForeground(new Color(70, 130, 180));

            // 设置按钮样式
            wordButton.setFont(boldFont);
            wordButton.setBackground(new Color(70, 130, 180));
            wordButton.setForeground(Color.WHITE);
            wordButton.setFocusPainted(false);
            wordButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
            wordButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // 设置标签样式
            label1.setFont(boldFont);
            label2.setFont(boldFont);
            label3.setFont(boldFont);
            label4.setFont(boldFont);
            label5.setFont(boldFont);

            // 设置面板背景色
            root.setBackground(new Color(240, 240, 240));
            left.setBackground(new Color(240, 240, 240));
            mid.setBackground(new Color(240, 240, 240));
            right.setBackground(new Color(240, 240, 240));
            rightUp.setBackground(new Color(240, 240, 240));

            // 设置间距
            root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            left.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            mid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            right.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            // 设置文本区域样式
            scanTextArea.setLineWrap(true);
            scanTextArea.setWrapStyleWord(true);
            styleTextArea(scanTextArea);
            styleTextArea(errorTextArea);
            styleTextArea(partitionTextArea);
            styleTextArea(signTableTextArea);
        }


    // 修改initSyntaxPanel方法中的面板设置
    private void initSyntaxPanel() {
        syntaxPanel.removeAll();
        syntaxPanel.setLayout(new BorderLayout());
        syntaxPanel.setBackground(new Color(240, 240, 240));

        // 主容器（左右两列布局）
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setResizeWeight(0.8);

        /* 左侧列：语法分析过程 + 按钮 */
        JPanel leftPanel = new JPanel(new BorderLayout()); // 增加组件间距
        leftPanel.setBackground(new Color(240, 240, 240));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 增加边距

        // 语法分析过程区域
        JPanel processPanel = new JPanel(new BorderLayout());


        // 使用与词法分析一致的标签样式
        JLabel processLabel = new JLabel("语法分析过程");
        processLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        processLabel.setForeground(Color.BLACK);
        processLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0)); // 增加标签下边距

        JTextArea processArea = new JTextArea();
        styleTextArea(processArea);

        processPanel.add(processLabel, BorderLayout.NORTH);
        processPanel.add(new JScrollPane(processArea), BorderLayout.CENTER);

        /* 右侧列：词法分析结果 + 错误列表 */
        JPanel rightPanel = new JPanel(new GridLayout(2, 1)); // 增加组件间距
        rightPanel.setBackground(new Color(240, 240, 240));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 增加边距

        // 词法分析结果区域
        JPanel lexResultPanel = new JPanel(new BorderLayout());
        lexResultPanel.setBackground(Color.WHITE);
        lexResultPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lexResultLabel = new JLabel("词法分析结果");
        lexResultLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        lexResultLabel.setForeground(Color.BLACK);
        lexResultLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JTextArea lexResultArea = new JTextArea();
        styleTextArea(lexResultArea);

        lexResultPanel.add(lexResultLabel, BorderLayout.NORTH);
        lexResultPanel.add(new JScrollPane(lexResultArea), BorderLayout.CENTER);

        // 错误列表区域
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBackground(Color.WHITE);
        errorPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 130, 180), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel errorLabel = new JLabel("错误列表");
        errorLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        errorLabel.setForeground(Color.BLACK);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JTextArea errorArea = new JTextArea();
        styleTextArea(errorArea);
        errorArea.setForeground(Color.RED);

        errorPanel.add(errorLabel, BorderLayout.NORTH);
        errorPanel.add(new JScrollPane(errorArea), BorderLayout.CENTER);

        rightPanel.add(lexResultPanel);
        rightPanel.add(errorPanel);

        // 设置左右面板
        mainSplitPane.setLeftComponent(leftPanel);
        mainSplitPane.setRightComponent(rightPanel);

        // 分析按钮（与词法分析按钮样式一致）
        JButton analyzeButton = new JButton("开始分析");
        styleButton(analyzeButton);
        analyzeButton.addActionListener(e -> {
            processArea.setText("语法分析过程将显示在这里...");
            lexResultArea.setText("词法分析结果将显示在这里...");
            errorArea.setText("错误信息将显示在这里...");
        });

        leftPanel.add(processPanel, BorderLayout.CENTER);
        leftPanel.add(analyzeButton, BorderLayout.SOUTH);

        syntaxPanel.add(mainSplitPane, BorderLayout.CENTER);
        syntaxPanel.revalidate();
        syntaxPanel.repaint();
    }

        // 设置文本区域样式（统一使用）
        private void styleTextArea(JTextArea textArea) {
            textArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setMargin(new Insets(5, 5, 5, 5));
        }

        // 设置按钮样式（统一使用）
        private void styleButton(JButton button) {
            button.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
            button.setBackground(new Color(70, 130, 180));
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
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

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
