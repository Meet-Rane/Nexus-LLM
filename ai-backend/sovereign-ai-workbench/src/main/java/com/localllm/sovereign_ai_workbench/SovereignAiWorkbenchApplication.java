package com.localllm.sovereign_ai_workbench;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SovereignAiWorkbenchApplication {

    static {
        System.setProperty("com.openai.timeout", "300000");
        System.setProperty("openai.timeout", "300000");
        System.setProperty("sun.net.client.defaultConnectTimeout", "60000");
        System.setProperty("sun.net.client.defaultReadTimeout", "300000");
    }

    public static void main(String[] args) {
        SpringApplication.run(SovereignAiWorkbenchApplication.class, args);
    }
}
