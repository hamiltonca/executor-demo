package com.compucafe.executordemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.compucafe.executordemo" })

public class ExecutorDemoApplication {

    public ExecutorDemoApplication() {
        super();
    }


    public static void main(String[] args) {
        SpringApplication.run(ExecutorDemoApplication.class, args);
    }
}
