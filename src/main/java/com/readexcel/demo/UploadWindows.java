package com.readexcel.demo;

import com.sun.java.swing.plaf.windows.WindowsMenuUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class UploadWindows extends JFrame {
    public static void main(String[] args) {
        JFrame jf = new JFrame("上传发票");
        final JTextField jt = new JTextField("", 100);
        final JButton jb = new JButton("提交");
        jb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String path = jt.getText();
                try {
                    readPdf2Excel.pdf2Excel(path);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        jf.setLayout(null);
        jf.setBounds(300, 100, 1000, 600);
        jt.setBounds(400, 100, 200, 50);
        jb.setBounds(400, 200, 200, 50);
        jf.add(jt);
        jf.add(jb);
        jf.setVisible(true);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
