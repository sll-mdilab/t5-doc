package net.sllmdilab.t5.processors;

import net.sllmdilab.commons.exceptions.T5Exception;
import net.sllmdilab.t5.converters.XMLToRDFConverter;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
public class TriplificationProcessor implements Processor {
	private static Logger logger = LoggerFactory.getLogger(TriplificationProcessor.class);

	@Autowired
	private XMLToRDFConverter xmlToRDFConverter;
	
	@Override
	public void process(Exchange exchange) {
		long startTime = System.currentTimeMillis();
		
		Document xmlBody;
		
		if(exchange.getIn().getBody() instanceof Document) {
			xmlBody = (Document) exchange.getIn().getBody();
		} else {
			throw new T5Exception("TriplificationProcessor can only handle XML document message body, got " + exchange.getIn().getBody().getClass().getName());
		}

		String triples = xmlToRDFConverter.triplifyXMLDoc(xmlBody);
		
		exchange.getOut().setBody(triples);
		exchange.getOut().setHeaders(exchange.getIn().getHeaders());
		
		logger.info("Triplification took " + (System.currentTimeMillis() - startTime) + "ms.");
	}
}
