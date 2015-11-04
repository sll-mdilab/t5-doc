package net.sllmdilab.t5;

import org.apache.camel.spring.boot.FatJarRouter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({ "net.sllmdilab.t5.*" })
public class T5FatJarRouter extends FatJarRouter {

}
