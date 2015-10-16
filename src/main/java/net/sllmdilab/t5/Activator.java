package net.sllmdilab.t5;

import java.net.URL;
import java.net.URLClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Configuration
public class Activator {
	private static Logger logger = LoggerFactory.getLogger(T5BackendApplication.class);

//	private AnnotationConfigApplicationContext applicationContext;

	public void start() throws Exception {
		
		logger.info("Starting application (as bundle).");
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
        	logger.info("CP" + url.getFile());
        }
		
//		applicationContext = new AnnotationConfigApplicationContext(
//				ApplicationConfiguration.class);
	}

	public void stop() throws Exception {
//		applicationContext.close();
	}
}
