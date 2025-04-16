package com.hyp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@EnableMongoAuditing
@EnableScheduling
@EnableCaching
@EnableAsync
@EnableRetry
@EnableAspectJAutoProxy
@OpenAPIDefinition(info = @Info(title = "Hyperapps Backend API", version = "1.0", description = "An APIs for Hyperapps Backend"),
servers = {
        @Server(url = "https://api.hyperapps.cloud/api/v2", description = "Staging API Server"),
        @Server(url = "https://api.hyperapps.in/api/v2", description = "Production API Server")
})
public class HypBackendApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HypBackendApiApplication.class, args);
	}

}
