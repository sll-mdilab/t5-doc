package net.sllmdilab.t5.components;

import java.util.Map;

import net.sllmdilab.t5.endpoints.MLDBEndpoint;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.XMLDocumentManager;

@Component(value = "mldb")
public class MLDBComponent extends DefaultComponent {
	@Autowired
	private DatabaseClient databaseClient;
	
	@Autowired
	private XMLDocumentManager documentManager;

	@Override
	protected Endpoint createEndpoint(String uri, String remaining,
			Map<String, Object> parameters) throws Exception {
		Endpoint endpoint = new MLDBEndpoint(uri, this, databaseClient, documentManager);
		setProperties(endpoint, parameters);
		
		return endpoint;
	}

}
