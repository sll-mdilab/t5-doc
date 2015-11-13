package net.sllmdilab.t5.processors;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import net.sllmdilab.t5.converters.PCD_01MessageToSqlObservationsConverter;
import net.sllmdilab.t5.dao.ObservationDao;
import net.sllmdilab.t5.dao.PCD01MessageDao;
import net.sllmdilab.t5.domain.SqlObservation;

@Component
public class SqlProcessor implements Processor {

	@Autowired
	private PCD_01MessageToSqlObservationsConverter obsConverter;
	
	@Autowired
	private PCD01MessageDao messageDao;
	
	@Autowired
	private ObservationDao obsDao;

	@Override
	public void process(Exchange exchange) throws Exception {
		if (exchange.getIn().getBody() instanceof Document) {
			Document message = (Document) exchange.getIn().getBody();
			
			List<SqlObservation> observations = obsConverter.convertToSqlObservations(message);
			
			long messageId = messageDao.insert(message);
			
			for(SqlObservation obs : observations) {
				obs.setMessageId(messageId);
				
				obsDao.insert(obs);

			}
		} else {
			throw new Exception("SqlProcessor can only handle XML document, got "
					+ exchange.getIn().getBody().getClass().getName());
		}
	}


}
