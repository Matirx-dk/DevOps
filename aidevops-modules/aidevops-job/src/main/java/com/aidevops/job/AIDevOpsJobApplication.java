package com.aidevops.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.aidevops.common.security.annotation.EnableCustomConfig;
import com.aidevops.common.security.annotation.EnableRyFeignClients;

/**
 * 定时任务
 * 
 * @author aidevops
 */
@EnableCustomConfig
@EnableRyFeignClients   
@SpringBootApplication
public class AIDevOpsJobApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AIDevOpsJobApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  定时任务模块Hermes Test Build v5 - Jenkinsfile parallel patch test ✓   ლ(´ڡ`ლ)ﾞ  \n" +
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
