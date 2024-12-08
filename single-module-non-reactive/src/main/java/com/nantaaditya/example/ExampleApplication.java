package com.nantaaditya.example;

import com.nantaaditya.example.properties.AsyncTaskProperties;
import com.nantaaditya.example.properties.LogProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableConfigurationProperties(value = {
		AsyncTaskProperties.class,
		LogProperties.class
})
@SpringBootApplication
public class ExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExampleApplication.class, args);
	}

}
