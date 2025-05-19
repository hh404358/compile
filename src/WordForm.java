import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.ParseException;
import java.util.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private JTextArea midCodeArea;


    // 存储词法分析的结果
    private List<Token> tokens;

    // 判断是否词法分析
    private boolean isSyntax=false;
    public WordForm() {
        tabbedPane.setTitleAt(0, "词法分析");
        tabbedPane.setTitleAt(1, "语法分析");
        // 统一设置全局样式
        setGlobalStyles();

        // 设置词法分析按钮事件
        wordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FullLR1Parser.resetCounters();
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

        // 初始化语法分析面板
        initSyntaxPanel();

        // 语法分析器初始化
        FullLR1Parser.initializeProductions();
        FullLR1Parser.computeFirstSets();
        FullLR1Parser.buildParser();

        // 初始化 LR(1) 分析表面板
        initTablePanel();

        // 初始化时同步内容
        lexResultArea.setText(partitionTextArea.getText());

        // 初始化中间代码面板
        initMidCodePanel();
    }

    // 修改initSyntaxPanel方法中的面板设置
    private void initSyntaxPanel() {
        syntaxPanel.removeAll();
        syntaxPanel.setLayout(new BorderLayout());
        syntaxPanel.setBackground(new Color(240, 240, 240));

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerSize(5);

            // 使用ComponentListener确保在布局完成后设置初始比例
        mainSplitPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    // 只设置一次初始位置
                    mainSplitPane.removeComponentListener(this);
                    mainSplitPane.setDividerLocation(0.7);
                }
        });
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
        // 创建表格
        String[] columnNames = {"状态栈", "符号栈", "输入串", "动作"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        JTable processTable = new JTable(tableModel);
//        styleTextArea(processArea);

        processPanel.add(processLabel, BorderLayout.NORTH);
        processPanel.add(new JScrollPane(processTable), BorderLayout.CENTER);

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
            tableModel.setRowCount(0);
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
            if (steps != null) {
                for (ParseStep step : steps) {
                    tableModel.addRow(new Object[]{
                            step.states,
                            step.symbols,
                            step.input,
                            step.action
                    });
                }
            }
            isSyntax=true;
        });

        leftPanel.add(processPanel, BorderLayout.CENTER);
        leftPanel.add(analyzeButton, BorderLayout.SOUTH);

        syntaxPanel.add(mainSplitPane, BorderLayout.CENTER);
        syntaxPanel.revalidate();
        syntaxPanel.repaint();
    }

    //初始化LR(1)表格
    private void initTablePanel() {
        // 清空 tablePanel
        tablePanel.removeAll();
        tablePanel.setLayout(new BorderLayout());
        tablePanel.setBackground(new Color(240, 240, 240));

        // 创建表格模型
        DefaultTableModel tableModel = new DefaultTableModel();

        // 添加表头
        List<String> terminals = FullLR1Parser.terminals;
        List<String> nonTerminals = FullLR1Parser.nonTerminals;

        // 第一列是状态
        String[] columnNames = new String[1 + terminals.size() + nonTerminals.size()];
        columnNames[0] = "State";
        for (int i = 0; i < terminals.size(); i++) {
            columnNames[i + 1] = terminals.get(i);
        }
        for (int i = 0; i < nonTerminals.size(); i++) {
            columnNames[i + terminals.size() + 1] = nonTerminals.get(i);
        }

        tableModel.setColumnIdentifiers(columnNames);

        // 添加表格数据
        Map<Integer, Map<String, String>> actionTable = FullLR1Parser.actionTable;
        Map<Integer, Map<String, Integer>> gotoTable = FullLR1Parser.gotoTable;

        for (int state = 0; state < FullLR1Parser.states.size(); state++) {
            Object[] row = new Object[columnNames.length];
            row[0] = state; // 状态

            // 填充 Action 表数据
            Map<String, String> actionMap = actionTable.getOrDefault(state, Collections.emptyMap());
            for (int i = 0; i < terminals.size(); i++) {
                String terminal = terminals.get(i);
                row[i + 1] = actionMap.getOrDefault(terminal, "");
            }

            // 填充 GOTO 表数据
            Map<String, Integer> gotoMap = gotoTable.getOrDefault(state, Collections.emptyMap());
            for (int i = 0; i < nonTerminals.size(); i++) {
                String nonTerminal = nonTerminals.get(i);
                Integer gotoState = gotoMap.getOrDefault(nonTerminal, -1);
                row[i + terminals.size() + 1] = (gotoState != -1) ? String.valueOf(gotoState) : "";
            }

            tableModel.addRow(row);
        }

        // 创建 JTable 并设置样式
        JTable lr1Table = new JTable(tableModel);
        lr1Table.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        lr1Table.setRowHeight(25);

        // 设置表头字体大小
        JTableHeader tableHeader = lr1Table.getTableHeader();
        tableHeader.setFont(new Font("Microsoft YaHei", Font.BOLD, 16)); // 设置表头字体大小为16

        // 设置每一列的固定宽度
        for (int i = 0; i < columnNames.length; i++) {
            TableColumn column = lr1Table.getColumnModel().getColumn(i);
            column.setPreferredWidth(100); // 设置每列宽度为100像素
            column.setMinWidth(80); // 设置最小宽度
            column.setMaxWidth(80); // 设置最大宽度
        }

        // 禁用 JTable 的自动调整模式
        lr1Table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // 添加滚动条
        JScrollPane scrollPane = new JScrollPane(lr1Table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS); // 强制始终显示水平滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); // 允许垂直滚动

        // 添加到 tablePanel
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.revalidate();
        tablePanel.repaint();
    }

    // 初始化中间代码面板
    private void initMidCodePanel() {
        midcodePanel.removeAll();
        midcodePanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 左侧面板（中间代码部分）
        JPanel leftPanel = new JPanel(new BorderLayout());

        // 中间代码标题标签
        JLabel codeTitleLabel = new JLabel("中间代码生成");
        codeTitleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        codeTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        codeTitleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        // 中间代码表格
        DefaultTableModel codeTableModel = new DefaultTableModel(
                new Object[]{"操作符(op)", "参数1(arg1)", "参数2(arg2)", "结果(result)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable codeTable = new JTable(codeTableModel);


        // 设置列宽
        codeTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        codeTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        codeTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        codeTable.getColumnModel().getColumn(3).setPreferredWidth(80);

        // 生成按钮
        JButton generateButton = new JButton("生成中间代码");
        styleButton(generateButton);

        // 右侧面板（错误列表部分）
        JPanel rightPanel = new JPanel(new BorderLayout());

        // 错误列表标题标签
        JLabel errorTitleLabel = new JLabel("错误列表");
        errorTitleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        errorTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        errorTitleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        DefaultListModel<String> errorListModel = new DefaultListModel<>();
        JList<String> errorList = new JList<>(errorListModel);
        errorList.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        errorList.setBackground(Color.WHITE);

        // 按钮点击事件
        generateButton.addActionListener(e -> {
            errorListModel.clear();
            codeTableModel.setRowCount(0);

            // 检查是否已完成词法和语法分析
            if (!isLexicalAnalysisDone() || !isSyntaxAnalysisDone()) {
                errorListModel.addElement("错误：请先完成词法分析和语法分析");
                return;
            }

            try {
                // 获取中间代码
                List<IntermediateCode> codes = FullLR1Parser.getIntermediateCode();
                List<String> SemanticErrors = FullLR1Parser.getSemanticErrors();
                if (codes.isEmpty()) {
                    errorListModel.addElement("提示：未生成中间代码");
                    return;
                }

                // 填充表格
                for (IntermediateCode code : codes) {
                    codeTableModel.addRow(new Object[]{
                            code.getOperator(),
                            code.getArg1(),
                            code.getArg2(),
                            code.getResult()
                    });
                }

                if (!SemanticErrors.isEmpty()) {
                    for (String error : SemanticErrors) {
                        errorListModel.addElement(error);
                    }
                }
            } catch (Exception ex) {
                errorListModel.addElement("错误：" + ex.getMessage());
            }
        });

        // 组装左侧面板
        leftPanel.add(codeTitleLabel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(codeTable), BorderLayout.CENTER);
        leftPanel.add(generateButton, BorderLayout.SOUTH);

        // 组装右侧面板
        rightPanel.add(errorTitleLabel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(errorList), BorderLayout.CENTER);

        // 添加到主面板
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.7; // 左侧占70%
        midcodePanel.add(leftPanel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3; // 右侧占30%
        midcodePanel.add(rightPanel, gbc);

        midcodePanel.revalidate();
        midcodePanel.repaint();
    }

    // 辅助方法：检查词法分析是否完成
    private boolean isLexicalAnalysisDone() {
        // 实现根据实际情况检查词法分析是否完成
        return tokens != null && !tokens.isEmpty();
    }

    // 辅助方法：检查语法分析是否完成
    private boolean isSyntaxAnalysisDone() {
        // 实现根据实际情况检查语法分析是否完成
        return isSyntax != false;
    }

    //设置全局样式
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

    // 设置滚动面板样式（统一使用）
    private JScrollPane styleScrollPane(JTextArea textArea, String title) {
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
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
