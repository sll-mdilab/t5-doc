package net.sllmdilab.t5.components;

import java.util.Map;

import net.sllmdilab.t5.endpoints.VirtuosoJenaRDFEndpoint;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.springframework.stereotype.Component;

@Component(value = "virtuoso")
public class VirtuosojenaRDFComponent extends DefaultComponent {

	@Override
	protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

		Endpoint endpoint = new VirtuosoJenaRDFEndpoint(uri, this);
		setProperties(endpoint, parameters);

		return endpoint;
	}

}
