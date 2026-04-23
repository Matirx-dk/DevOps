package com.aidevops.gen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.aidevops.common.security.annotation.EnableCustomConfig;
import com.aidevops.common.security.annotation.EnableRyFeignClients;

/**
 * 代码生成
 * 
 * @author aidevops
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class AIDevOpsGenApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AIDevOpsGenApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  代码生成模块Hermes Test Build - ✓   ლ(´ڡ`ლ)ﾞ  \n" +
                " .-------.       ____     __        \n" +
                " |  _ _   \\      \\   \\   /  /    \n" +
                " | ( ' )  |       \\  _. /  '       \n" +
                " |(_ o _) /        _( )_ .'         \n" +
                " | (_,_).' __  ___(_ o _)'          \n" +
                " |  |\\ \\  |  ||   |(_,_)'         \n" +
                " |  | \\ `'   /|   `-'  /           \n" +
                " |  |  \\    /  \\      /           \n" +
                " ''-'   `'-'    `-..-'              ");
    }
}
