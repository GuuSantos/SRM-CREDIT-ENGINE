package br.com.srm.creditengine;

import org.springframework.boot.SpringApplication;

public class TestBackendApplication {

    public static void main(String[] args) {
        SpringApplication.from(CreditEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
