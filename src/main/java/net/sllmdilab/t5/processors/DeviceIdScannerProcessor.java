package net.sllmdilab.t5.processors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.datatype.EI;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;

@Component
public class DeviceIdScannerProcessor implements Processor {
	private static Logger logger = LoggerFactory.getLogger(DeviceIdScannerProcessor.class);
	
	public static final String DEVICE_ID_LIST_HEADER = "device_id_list";

	@Override
	public void process(Exchange exchange) throws Exception {
		if (!(exchange.getIn().getBody() instanceof ORU_R01)) {
			logger.warn("Got non-ORU_R01 message.");
		} else {
			ORU_R01 message = (ORU_R01) exchange.getIn().getBody();

			exchange.getIn().setHeader(DEVICE_ID_LIST_HEADER, getDeviceIds(message));
		}
	}
	
	private List<String> getDeviceIds(ORU_R01 message) {
		Set<String> deviceIds = new HashSet<>();

		try {
			for (ORU_R01_PATIENT_RESULT patientResult : message.getPATIENT_RESULTAll()) {
				for (ORU_R01_ORDER_OBSERVATION orderObservation : patientResult.getORDER_OBSERVATIONAll()) {
					for (ORU_R01_OBSERVATION observation : orderObservation.getOBSERVATIONAll()) {
						deviceIds.addAll(extractDeviceIds(observation));
					}
				}
			}
		} catch (HL7Exception e) {
			logger.error("Exception while finding device id.", e);
		}

		if (logger.isDebugEnabled()) {
			for (String deviceId : deviceIds) {
				logger.debug("Found device: " + deviceId);
			}
		}

		return new ArrayList<String>(deviceIds);
	}

	private Set<String> extractDeviceIds(ORU_R01_OBSERVATION observation) {
		Set<String> deviceIds = new HashSet<>();

		for (EI ei : observation.getOBX().getObx18_EquipmentInstanceIdentifier()) {
			if (!StringUtils.isBlank(ei.getEi1_EntityIdentifier().getValue())) {
				deviceIds.add(ei.getEi1_EntityIdentifier().getValue());
			}
		}

		return deviceIds;
	}

}
