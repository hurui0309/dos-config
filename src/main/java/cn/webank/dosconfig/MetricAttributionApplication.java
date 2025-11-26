package cn.webank.dosconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 指标异动根因定位系统启动类
 */
@SpringBootApplication
@EnableAsync
public class MetricAttributionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetricAttributionApplication.class, args);
    }
}

