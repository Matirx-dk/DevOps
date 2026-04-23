package com.aidevops.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 文件服务
 * 
 * @author aidevops
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class AIDevOpsFileApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AIDevOpsFileApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  文件服务模块Hermes Test Build v4 - full rollout test ✓   ლ(´ڡ`ლ)ﾞ  \n" +
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
