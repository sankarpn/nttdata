package com.nttdata.documentqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DocumentQaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentQaServiceApplication.class, args);
	}

}
