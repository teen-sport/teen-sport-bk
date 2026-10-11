package com.uniquindio.teen_sport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TeenSportApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeenSportApplication.class, args);
	}

}
