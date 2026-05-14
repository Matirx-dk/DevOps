package com.aidevops.auth.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class SonarTest {

    private String password = "root123"; // 硬编码密码 - 安全漏洞

    // SQL注入漏洞
    public void login(String username, String password) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "root", this.password);
        Statement stmt = conn.createStatement();
        String sql = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
        ResultSet rs = stmt.executeQuery(sql); // SQL注入漏洞
        if (rs.next()) {
            System.out.println("登录成功");
        }
    }

    // 空指针异常风险
    public String getUserName(String input) {
        String str = null;
        if (input != null) {
            str = input.trim();
        }
        return str.toString(); // 可能 NPE
    }

    // 资源未关闭
    public void readFile(String path) throws Exception {
        Statement stmt = DriverManager.getConnection("", "", "").createStatement();
        ResultSet rs = stmt.executeQuery("SELECT 1");
        while (rs.next()) {
            System.out.println(rs.getString(1));
        }
        // 未关闭 rs、stmt、conn
    }

    // 硬编码密钥
    public String decrypt(String data) {
        String key = "mySecretKey12345"; // 密钥硬编码
        return data;
    }

    // 捕获异常后不处理
    public void process() {
        try {
            int i = 10 / 0;
        } catch (Exception e) {
            // 空catch块，不处理异常
        }
    }

    // 重复代码
    public void log() {
        System.out.println("log1");
        System.out.println("log2");
        System.out.println("log1");
        System.out.println("log2");
    }

    public static void main(String[] args) {
        SonarTest test = new SonarTest();
        test.process();
    }
}