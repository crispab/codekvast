package se.crisp.sample.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class SampleSpringBootApp {

	public static void main(String[] args) {
		SpringApplication.run(SampleSpringBootApp.class, args);
	}
}
