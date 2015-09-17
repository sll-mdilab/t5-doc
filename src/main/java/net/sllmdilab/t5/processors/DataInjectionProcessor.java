package net.sllmdilab.t5.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.datatype.ID;
import ca.uhn.hl7v2.model.v26.datatype.ST;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;

@Component
public class DataInjectionProcessor implements Processor {

	@Value("${T5_DATA_INJECTION_ENABLED}")
	private boolean dataInjectionEnabled;

	private static Logger logger = LoggerFactory.getLogger(DataInjectionProcessor.class);

	public static final String DEFAULT_CODING_SYSTEM = "MDC";
	public static final String DEFAULT_PATIENT_ID = "010101-2425";

	@Override
	public void process(Exchange exchange) throws Exception {
		
		if (dataInjectionEnabled) {
			Message message = exchange.getIn().getBody(Message.class);
			ORU_R01 oruMessage = (ORU_R01) message;

			String equipmentId = extractEquipmentIdentifier(oruMessage);
			if (!StringUtils.isBlank(equipmentId)) {
				setOBX18IfMissing(oruMessage, equipmentId);
			}
			handleObservations(oruMessage);
		}
	}

	private void handleObservations(ORU_R01 oruMessage) throws HL7Exception, DataTypeException {
		int obsCounter = 1;
		for (ORU_R01_OBSERVATION observation : oruMessage.getPATIENT_RESULT().getORDER_OBSERVATION()
				.getOBSERVATIONAll()) {

			injectCodingSystemIfMissing(observation);

			injectUnitCodingSystemIfMissing(observation);

			ST observationSubId = observation.getOBX().getObservationSubID();
			if (observationSubId.isEmpty()) {
				String hierarchy = "1.1.1." + (obsCounter++);
				logger.info("OBX-4 Observation sub-ID missing, inserting \"" + hierarchy + "\"");
				observationSubId.setValue(hierarchy);
			} else if (observationSubId.getValue().matches("\\d+")) {
				String hierarchy = "1.1." + observationSubId.getValue() + "." + (obsCounter++);
				logger.info("OBX-4 Observation sub-ID missing, inserting \"" + hierarchy + "\"");
				observationSubId.setValue(hierarchy);
			}
		}
	}

	private void setOBX18IfMissing(ORU_R01 oruMessage, String value) throws HL7Exception {
		for (ORU_R01_OBSERVATION observation : oruMessage.getPATIENT_RESULT().getORDER_OBSERVATION()
				.getOBSERVATIONAll()) {
			if (observation.getOBX().getObx18_EquipmentInstanceIdentifierReps() == 0) {
				logger.info("OBX-18-1 Equipment Instance Identifier missing, inserting \"" + value + "\"");
				observation.getOBX().getObx18_EquipmentInstanceIdentifier(0).getEi1_EntityIdentifier().setValue(value);
			}
		}
	}

	private void injectUnitCodingSystemIfMissing(ORU_R01_OBSERVATION observation) throws HL7Exception,
			DataTypeException {
		ID unitCodingSystem = observation.getOBX().getUnits().getNameOfCodingSystem();
		if (unitCodingSystem.isEmpty()) {
			logger.info("OBX-6.3 Unit coding system missing, inserting \"" + DEFAULT_CODING_SYSTEM + "\"");
			unitCodingSystem.setValue(DEFAULT_CODING_SYSTEM);
		}
	}

	private void injectCodingSystemIfMissing(ORU_R01_OBSERVATION observation) throws HL7Exception, DataTypeException {
		ID codingSystem = observation.getOBX().getObx3_ObservationIdentifier().getNameOfCodingSystem();
		if (codingSystem.isEmpty()) {
			logger.info("OBX-3.3 Coding system missing, inserting \"" + DEFAULT_CODING_SYSTEM + "\"");
			codingSystem.setValue(DEFAULT_CODING_SYSTEM);
		}
	}

	private String extractEquipmentIdentifier(ORU_R01 oruMessage) {
		return oruMessage.getPATIENT_RESULT().getORDER_OBSERVATION().getOBR().getObr10_CollectorIdentifier(0)
				.getXcn1_IDNumber().getValue();
	}
}
