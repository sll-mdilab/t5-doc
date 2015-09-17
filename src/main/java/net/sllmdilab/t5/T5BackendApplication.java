package net.sllmdilab.t5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
public class T5BackendApplication {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(T5BackendApplication.class);
		logger.info("Starting application.");
		
		new AnnotationConfigApplicationContext(
				T5BackendApplication.class);
	}
}
