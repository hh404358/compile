import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

/**
 * @Author: hahaha
 * @Package:PACKAGE_NAME
 * @Project:word_translator
 * @name:WordGUI
 * @Date:2025/4/21
 * @Time:15:38
 * @Filename:WordGUI
 **/
public class WordGUI extends JFrame implements ActionListener//继承JFrame顶层框架,实现监听器
{

    //定义组件
    //上部组件
    JPanel panel1;        //定义面板
    JPanel panel2;
    JSplitPane jsplitpane;    //定义拆分窗格
    JTextArea complier_text;    //定义编译器返回框
    JScrollPane jspane1;    //定义滚动窗格
    JTextArea input_text;    //定义输入框
    JScrollPane jspane2;
    //下部组件
    JButton jb1, send;    //定义按钮
    JComboBox jcb1, jcb2;        //定义下拉框?

//        public static void main(String[] args) {
//            Word_GUI gui = new Word_GUI();    //显示界面
//        }

    public WordGUI()        //构造函数
    {
        //创建组件
        //上部组件
        panel1 = new JPanel();    //创建面板
        complier_text = new JTextArea("");    //创建多行文本框
        complier_text.setLineWrap(true);    //设置多行文本框自动换行
        jspane1 = new JScrollPane(complier_text);    //创建滚动窗格
        input_text = new JTextArea("");
        input_text.setLineWrap(true);
        jspane2 = new JScrollPane(input_text);
        jsplitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jspane1, jspane2); //创建拆分窗格
        jsplitpane.setDividerLocation(400);    //设置拆分窗格分频器初始位置
        jsplitpane.setDividerSize(1);//设置分屏器大小
        complier_text.setEditable(false); //设置编译框不可编辑
        Font f0 = new Font("宋体", Font.PLAIN, 40);      //输入框初始化字体
        Font f_button = new Font("宋体", Font.PLAIN, 18);    //按钮字体
        //下部组件
        panel2 = new JPanel();
        jb1 = new JButton("应用");        //创建按钮
        send = new JButton("编译运行");
        jb1.setFont(f_button);        //按钮字体设置
        send.setFont(f_button);
        String[] name1 = {" ---输入框字体大小--- ", "20", "25", "30", "35", "40", "45", "50"};//下拉框文本内容
        String[] name2 = {" ---编译框字体大小--- ", "20", "25", "30", "35", "40", "45", "50"};
        jcb1 = new JComboBox(name1);    //创建下拉框
        jcb2 = new JComboBox(name2);
        jcb1.setFont(f_button);    //下拉框字体设置
        jcb2.setFont(f_button);
        send.setBorderPainted(false);
        jcb1.setBackground(Color.blue);
        send.setBackground(Color.white);
        panel2.setBackground(Color.white);
        jb1.addActionListener(new ActionListener()//设置按钮监听和内部匿名类实现监听事件
        {
            public void actionPerformed(ActionEvent e) {
                int size1, size2;
                String S1 = jcb1.getSelectedItem().toString();
                String S2 = jcb2.getSelectedItem().toString();
                if (S1.length() < 4) {
                    size1 = Integer.parseInt(S1);
                    Font f1 = new Font("宋体", Font.PLAIN, size1);
                    input_text.setFont(f1);
                }
                if (S2.length() < 4) {
                    size2 = Integer.parseInt(S2);
                    Font f2 = new Font("宋体", Font.PLAIN, size2);
                    complier_text.setFont(f2);
                }
            }
        });
        input_text.setFont(f0);//输入框和编译框字体初始化
        complier_text.setFont(f0);
        panel1.setLayout(new BorderLayout());    //设置布局管理，面板布局
        panel2.setLayout(new FlowLayout(FlowLayout.RIGHT));
        panel1.add(jsplitpane);//添加组件
        panel2.add(jcb2);
        panel2.add(jcb1);
        panel2.add(jb1);
        panel2.add(send);
        this.add(panel1, BorderLayout.CENTER);
        this.add(panel2, BorderLayout.SOUTH);
        this.setTitle("词法分析");//设置界面标题
//		this.setIconImage(new ImageIcon("image/qq.gif").getImage());//设置标题图片
        this.setSize(1300, 800);//设置界面像素
        this.setLocation(200, 80);//设置界面初始位置
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//设置虚拟机和界面一同关闭
        this.setVisible(true);//设置界面可视化
        send.addActionListener(this);//设置按钮监听
    }

    public void actionPerformed(ActionEvent e)//检测到点击按钮时进行的动作
    {
        if (e.getSource() == send) {
            WordAnalysis analysis = new WordAnalysis();//创建词法分析对象
            String input = input_text.getText();//获取词法输入内容
            try {
                analysis.analyze(input);
                String result_after_deal = analysis.getResults();
                complier_text.setText(result_after_deal);//输出词法分析结果
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }
    }
    public static void main(String[] args) {
        WordGUI gui = new WordGUI();    //显示界面
    }
}
