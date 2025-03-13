package com.hmdp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 启动类
 *
 * @author CHEN
 * @date 2022/10/07
 */
@MapperScan("com.hmdp.mapper")
@SpringBootApplication
public class HmDianPingApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(HmDianPingApplication.class, args);
         // 添加关闭钩子，确保资源优雅关闭
         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("应用正在关闭，执行资源清理...");
                if (applicationContext != null && applicationContext.isActive()) {
                    // 通过应用上下文正确关闭
                    applicationContext.close();
                    System.out.println("应用已关闭");
                }
            } catch (Exception e) {
                System.err.println("应用关闭过程中发生错误: " + e.getMessage());
            }
        }));
    }
}


