package com.hyp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class HypBackendApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HypBackendApiApplication.class, args);
	}

}
