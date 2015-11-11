package net.sllmdilab.t5.processors;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import net.sll_mdilab.t5.EquipmentIdentifier;
import net.sll_mdilab.t5.Observation;
import net.sll_mdilab.t5.PCD01Message;
import net.sllmdilab.t5.dao.PCD01MessageDao;
import net.sllmdilab.t5.domain.SqlObservation;

@Component
public class SqlProcessor implements Processor {
	@Autowired
	private PCD01MessageDao messageDao;

	@Override
	public void process(Exchange exchange) throws Exception {
		if (exchange.getIn().getBody() instanceof Document) {

			messageDao.insert((Document) exchange.getIn().getBody());
		} else {
			throw new Exception("SqlProcessor can only handle XML document, got "
					+ exchange.getIn().getBody().getClass().getName());
		}
	}

	private static class IdItem {
		public final String level;
		public final String id;

		private IdItem(String level, String id) {
			this.level = level;
			this.id = id;
		}

		public static IdItem of(String level, String id) {
			return new IdItem(level, id);
		}
	}

	private void handleObservations(Document document) throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(PCD01Message.class);
		Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

		Deque<IdItem> idStack = new LinkedList<>();

		PCD01Message message = (PCD01Message) unmarshaller.unmarshal(document);

		message.getPatientResult().forEach(patientResult -> {
			patientResult.getOrderObservations().forEach(orderObs -> {
				orderObs.getMDS().forEach(mds -> {

					List<IdItem> items = findIds("MDS", mds.getVMDAndObservation());

					items.forEach(item -> idStack.push(item));
					
				});
			});
		});
	}
	
	private List<SqlObservation> handleVmd(List<Object> objects) {
		List<SqlObservation> result = new ArrayList<>();
		objects.forEach(object -> {
			if (object instanceof Observation) {
				Observation obs = (Observation) object;
				if(obs.getValue() != null && !obs.getValue().isEmpty()) {
					
				}
			}
		});
		
		return result;
	}

	private List<IdItem> findIds(String level, List<Object> objects) {
		List<IdItem> result = new ArrayList<>();
		objects.forEach(object -> {
			if (object instanceof Observation) {
				Observation obs = (Observation) object;
				EquipmentIdentifier equipmentIdentifier = obs.getEquipmentIdentifier();
				if (equipmentIdentifier != null) {
					String id = equipmentIdentifier.getValue();
					if (!StringUtils.isBlank(id)) {
						result.add(IdItem.of(level, id));
					}
				}
			}
		});

		return result;
	}
}
