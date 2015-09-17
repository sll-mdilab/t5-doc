package net.sllmdilab.t5.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import ca.uhn.hl7v2.parser.XMLParser;

@Component
public class StandardHL7XMLProcessor implements Processor {
	private static Logger logger = LoggerFactory.getLogger(StandardHL7XMLProcessor.class);
	
	@Override
	public void process(Exchange exchange) throws Exception {
		
		Message message = exchange.getIn().getBody(Message.class);
		
		XMLParser xmlparser = new DefaultXMLParser();
		Document document = xmlparser.encodeDocument(message);
		if(logger.isTraceEnabled()) {
			logger.trace("HL7v2 XML: " + document.toString());
		}
		
		exchange.getOut().setBody(document);
		exchange.getOut().setHeaders(exchange.getIn().getHeaders());
	}
}
