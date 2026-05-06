package com.semse.mobile_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MobileServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MobileServerApplication.class, args);
	}

}
