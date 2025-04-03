package org.example.Main;

import org.example.MyBatisConfig.MybatisDbHandler;

import java.io.IOException;

public class main {
    public static void main(String[] args) throws IOException {
        MybatisDbHandler mybatisDbHandler = new MybatisDbHandler();

        mybatisDbHandler.MybatisDAO();
    }
}
