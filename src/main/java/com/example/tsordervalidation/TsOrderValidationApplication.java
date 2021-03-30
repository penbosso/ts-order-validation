package com.example.tsordervalidation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SpringBootApplication
public class TsOrderValidationApplication {

	public static void main(String[] args) {
		SpringApplication.run(TsOrderValidationApplication.class, args);
	}

	@PostConstruct
	public void startRedis() {
	System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
	System.out.println("^^Delete subscription");
	System.out.println("^^subscribe^^^^^^^");
	System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
	}
}
