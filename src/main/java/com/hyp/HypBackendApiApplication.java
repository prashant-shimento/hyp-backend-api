package com.hyp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@EnableMongoAuditing
@OpenAPIDefinition(info = @Info(title = "Menu API", version = "2.0", description = "An API for Managing Restaurant Menus"))
public class HypBackendApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HypBackendApiApplication.class, args);
	}

}
