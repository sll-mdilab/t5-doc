package net.sllmdilab.t5.producer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import net.sllmdilab.t5.components.MLDBComponent;
import net.sllmdilab.t5.endpoints.MLDBEndpoint;
import net.sllmdilab.t5.producer.MLDBProducer;
import net.sllmdilab.t5.test.TestUtils;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultEndpointConfiguration;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.impl.DocumentUriTemplateImpl;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.marker.XMLWriteHandle;

public class MLDBProducerTest {
	private static final String DUMMY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<test/>";

	@Mock
	private DatabaseClient databaseClient;

	@Mock
	private XMLDocumentManager documentManager;

	private MLDBProducer mldbProducer;
	private MLDBEndpoint mldbEndpoint;
	private CamelContext camelContext;

	@Before
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		when(documentManager.newDocumentUriTemplate(any())).thenReturn(new DocumentUriTemplateImpl("xml"));
		
		camelContext = new DefaultCamelContext();
		
		MLDBComponent mldbComponent = new MLDBComponent();
		mldbComponent.setCamelContext(camelContext);
		mldbEndpoint = new MLDBEndpoint("", mldbComponent,
				databaseClient, documentManager);
		mldbEndpoint.setEndpointConfiguration(new DefaultEndpointConfiguration(camelContext, "mldb://standardxml") {
			@Override
			public String toUriString(UriFormat format) {
				return "mldb:standardxml";
			}
		});
		mldbProducer = (MLDBProducer) mldbEndpoint.createProducer();
	}

	@Test
	public void processExchange() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		Document d = TestUtils.xmlStringToDocument(DUMMY_XML);
		message.setBody(d);
		exchange.setIn(message);
		mldbProducer.process(exchange);
		
		ArgumentCaptor<XMLWriteHandle> argument = ArgumentCaptor.forClass(XMLWriteHandle.class);
		verify(documentManager).create(any(), argument.capture());
		
		assertEquals(DUMMY_XML, ((DOMHandle)argument.getValue()).toString());
	}
}
