package com.aidevops.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

/**
 * 网关启动程序
 * 
 * @author aidevops
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class AIDevOpsGatewayApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(AIDevOpsGatewayApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  AI智能运维网关Hermes Test Build - ✓   ლ(´ڡ`ლ)ﾞ  \n" +
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
