package net.sllmdilab.t5.processors;

import net.sllmdilab.t5.converters.PCD_01MessageToXMLConverter;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import ca.uhn.hl7v2.model.Message;

@Component
public class T5XMLProcessor implements Processor {
	private static Logger logger = LoggerFactory.getLogger(T5XMLProcessor.class);
	
	@Autowired
	private PCD_01MessageToXMLConverter pcd_01MessageToXMLConverter;

	@Override
	public void process(Exchange exchange) {
		Message message = exchange.getIn().getBody(Message.class);

		Document document = pcd_01MessageToXMLConverter.getXML(message);
		
		if(logger.isTraceEnabled()) {
			logger.trace("T5 XML: " + document.toString());
		}
		
		exchange.getOut().setBody(document);
		exchange.getOut().setHeaders(exchange.getIn().getHeaders());
	}

}
