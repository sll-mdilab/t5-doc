package net.sllmdilab.t5.producer;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.w3c.dom.Document;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentUriTemplate;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.marker.XMLWriteHandle;

public class MLDBProducer extends DefaultProducer {

	private XMLDocumentManager documentManager;

	public MLDBProducer(Endpoint endpoint, DatabaseClient databaseClient,
			XMLDocumentManager documentManager) {
		super(endpoint);

		this.documentManager = documentManager;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		XMLWriteHandle writeHandle = getWriteHandleFromBody(exchange);
		
		String directory = getEndpoint().getEndpointConfiguration().getURI()
				.getHost();

		DocumentUriTemplate template = documentManager
				.newDocumentUriTemplate("xml");
		template.setDirectory(directory + "/");

		documentManager.create(template, writeHandle);
	}

	private XMLWriteHandle getWriteHandleFromBody(Exchange exchange)
			throws Exception {
		XMLWriteHandle writeHandle;
		
		if(exchange.getIn().getBody() instanceof String) {
			StringHandle stringHandle = new StringHandle((String) exchange.getIn().getBody());
			stringHandle.setFormat(Format.XML);
			
			writeHandle = stringHandle;
		} else if (exchange.getIn().getBody() instanceof Document) {
			DOMHandle domHandle = new DOMHandle((Document) exchange.getIn().getBody());
			domHandle.setFormat(Format.XML);
			
			writeHandle = domHandle;
		} else {
			throw new Exception("MLDBProducer can only handle String or XML document, got " + exchange.getIn().getBody().getClass().getName());
		}
		return writeHandle;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}
	
	
}
