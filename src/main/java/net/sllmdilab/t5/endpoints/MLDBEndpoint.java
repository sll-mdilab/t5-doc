package net.sllmdilab.t5.endpoints;

import net.sllmdilab.t5.components.MLDBComponent;
import net.sllmdilab.t5.producer.MLDBProducer;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.XMLDocumentManager;

public class MLDBEndpoint extends DefaultEndpoint {
	private DatabaseClient databaseClient;
	private XMLDocumentManager documentManager;

	public MLDBEndpoint() {
	}

	public MLDBEndpoint(String uri, MLDBComponent component, DatabaseClient databaseClient,
			XMLDocumentManager documentManager) {
		super(uri, component);

		this.databaseClient = databaseClient;
		this.documentManager = documentManager;
	}

	@Override
	public Producer createProducer() throws Exception {
		return new MLDBProducer(this, databaseClient, documentManager);
	}

	@Override
	public Consumer createConsumer(Processor processor) throws Exception {
		throw new UnsupportedOperationException("No consumer for this endpoint.");
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
