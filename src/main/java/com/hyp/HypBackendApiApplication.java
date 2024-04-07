package com.hyp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableMongoAuditing
@OpenAPIDefinition(info = @Info(title = "Hyperapps Backend API", version = "1.0", description = "An APIs for Hyperapps Backend"))
public class HypBackendApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HypBackendApiApplication.class, args);
	}

}
