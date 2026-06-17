package com.example.delhi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DelhiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DelhiApplication.class, args);
	}

}
