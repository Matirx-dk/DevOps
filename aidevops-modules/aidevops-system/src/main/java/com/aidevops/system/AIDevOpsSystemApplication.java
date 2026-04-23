package com.aidevops.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.aidevops.common.security.annotation.EnableCustomConfig;
import com.aidevops.common.security.annotation.EnableRyFeignClients;

/**
 * 系统模块
 * 
 * @author aidevops
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class AIDevOpsSystemApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AIDevOpsSystemApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  系统模块Hermes Test Build v6 - parallel patch + probe test ✓   ლ(´ڡ`ლ)ﾞ  \n" +
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
