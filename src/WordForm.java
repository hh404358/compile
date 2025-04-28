import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.ParseException;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


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
    private JTextArea lexResultArea;

    // 存储词法分析的结果
    private List<Token> tokens;

    public WordForm() {
        tabbedPane.setTitleAt(0, "词法分析");
        tabbedPane.setTitleAt(1, "语法分析");

        // 初始化语法分析面板
        initSyntaxPanel();

        // 语法分析器初始化
        FullLR1Parser.initializeProductions();
        FullLR1Parser.computeFirstSets();
        FullLR1Parser.buildParser();

        // 初始化时同步内容
        lexResultArea.setText(partitionTextArea.getText());

        // 统一设置全局样式
        setGlobalStyles();

        // 设置词法分析按钮事件
        wordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EnhancedLexer analysis = new EnhancedLexer();
                String input = inputTextArea.getText();
                String scan_result = analysis.pre(input);
                tokens = analysis.analyze(input); // 存储词法分析器结果
                String partition_result = analysis.getTokenList();
                String sign_table_result = analysis.getSymbolTable();
                String error_result = analysis.getErrorList();

                scanTextArea.setText(scan_result);
                errorTextArea.setText(error_result);
                partitionTextArea.setText(partition_result);
                signTableTextArea.setText(sign_table_result);

                // 更新语法分析面板中的词法分析结果
                lexResultArea.setText(partition_result);
            }
        });

        // 初始化表格滚动面板
        JScrollPane tableScrollPane = new JScrollPane(table1);
        tableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS); // 始终显示垂直滚动条
        tableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); // 始终显示水平滚动条

        // 清空并重新设置tablePanel布局
        tablePanel.removeAll();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.add(tableScrollPane, BorderLayout.CENTER);
        tablePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        initTableSettings();
        loadCsvToTable("src/LR1table.csv");
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

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerSize(5);
        mainSplitPane.setResizeWeight(0.8);

        /* 左侧列：语法分析过程 + 按钮 */
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(new Color(240, 240, 240));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 语法分析过程区域
        JPanel processPanel = new JPanel(new BorderLayout());

        JLabel processLabel = new JLabel("语法分析过程");
        processLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        processLabel.setForeground(Color.BLACK);
        processLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JTextArea processArea = new JTextArea();
        styleTextArea(processArea);

        processPanel.add(processLabel, BorderLayout.NORTH);
        processPanel.add(new JScrollPane(processArea), BorderLayout.CENTER);

        /* 右侧列：词法分析结果 + 错误列表 */
        JPanel rightPanel = new JPanel(new GridLayout(2, 1));
        rightPanel.setBackground(new Color(240, 240, 240));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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

        lexResultArea = new JTextArea(); // 初始化成员变量
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

        // 分析按钮
        JButton analyzeButton = new JButton("语法分析");
        styleButton(analyzeButton);
        analyzeButton.addActionListener(e -> {
            if (tokens == null) {
                errorArea.setText("请先进行词法分析");
                return;
            }
            // 清空输出
            processArea.setText("");
            errorArea.setText("");
            List<ParseStep> steps = null;
            try {
                steps = FullLR1Parser.parse(tokens);
            } catch (LR1ParserException ex) {
                steps = ex.getSteps();
                errorArea.setText(ex.getMessage());
            } catch (Exception ex) {
                errorArea.setText(ex.getMessage());
            }
            // 显示分析结果
            StringBuilder sb = new StringBuilder();
            for (ParseStep step : steps) {
                sb.append("状态栈: " + step.states + ", 符号栈: " + step.symbols + ", 输入串: " + step.input + ", 动作: " + step.action + '\n');
            }
            processArea.setText(sb.toString());
        });

        leftPanel.add(processPanel, BorderLayout.CENTER);
        leftPanel.add(analyzeButton, BorderLayout.SOUTH);

        syntaxPanel.add(mainSplitPane, BorderLayout.CENTER);
        syntaxPanel.revalidate();
        syntaxPanel.repaint();
    }

        private void initTableSettings() {
        // 启用表格编辑
        table1.setEnabled(true);
        table1.setDefaultEditor(Object.class, new DefaultCellEditor(new JTextField()));

        // 设置列宽自适应内容
        table1.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // 必须先禁用自动调整
        JScrollPane scrollPane = new JScrollPane(table1);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
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

        private void styleTable(JTable table) {
        table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        table.setRowHeight(25);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // 关键！禁用自动调整列宽
        table.setSelectionBackground(new Color(70, 130, 180));

        // 启用表格拖动手势（可选）
        table.setDragEnabled(true);
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


    private void loadCsvToTable(String csvFilePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            // 读取表头（第一行）
            String[] headers = br.readLine().split(",");

            // 读取数据行
            List<String[]> data = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                data.add(line.split(","));
            }

            // 创建 TableModel 并设置到 JTable
            DefaultTableModel model = new DefaultTableModel(data.toArray(new String[0][]), headers);
            table1.setModel(model);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(root, "加载 CSV 失败: " + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}
