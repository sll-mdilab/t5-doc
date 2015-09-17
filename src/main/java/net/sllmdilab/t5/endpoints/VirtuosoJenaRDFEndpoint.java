package net.sllmdilab.t5.endpoints;

import java.net.URI;

import net.sllmdilab.t5.components.VirtuosojenaRDFComponent;
import net.sllmdilab.t5.producer.VirtuosoJenaRDFProducer;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

public class VirtuosoJenaRDFEndpoint extends DefaultEndpoint {

	public VirtuosoJenaRDFEndpoint(String uri, VirtuosojenaRDFComponent component) {
		super(uri, component);
	}

	@Override
	public Producer createProducer() throws Exception {
		URI uri = getEndpointConfiguration().getURI();
		return new VirtuosoJenaRDFProducer(this, stripInitialSlash(uri.getPath()), uri.getHost(), uri.getPort(),
				getUser(uri.getUserInfo()), getPassword(uri.getUserInfo()));
	}

	@Override
	public Consumer createConsumer(Processor processor) throws Exception {
		throw new UnsupportedOperationException("No consumer for this endpoint.");
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	private String getUser(String userInfo) {
		if (!userInfo.contains(":")) {
			return userInfo;
		}
		return userInfo.substring(0, userInfo.indexOf(":"));
	}

	private String getPassword(String userInfo) {
		if (!userInfo.contains(":") || userInfo.indexOf(":") == userInfo.length() - 1) {
			return "";
		}

		String[] uiparts = userInfo.split(":");
		return uiparts[1];
	}

	private String stripInitialSlash(String path) {
		if (path.startsWith("/")) {
			return path.substring(1);
		} else {
			return path;
		}
	}
}
