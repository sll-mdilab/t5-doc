package net.sllmdilab.t5;

import org.apache.camel.spring.boot.FatJarRouter;
import org.apache.camel.spring.boot.FatWarInitializer;

public class T5FatWarRouter extends FatWarInitializer {

	@Override
	protected Class<? extends FatJarRouter> routerClass() {
		return T5FatJarRouter.class;
	}
}
