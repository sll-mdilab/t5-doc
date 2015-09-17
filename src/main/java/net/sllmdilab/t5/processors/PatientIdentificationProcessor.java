package net.sllmdilab.t5.processors;

import java.util.List;

import net.sllmdilab.t5.dao.PatientIdentificationDao;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.PID;

@Component
public class PatientIdentificationProcessor implements Processor {
	private static final String PATIENT_RESOURCE_PREFIX = "Patient/";

	private static Logger logger = LoggerFactory.getLogger(PatientIdentificationProcessor.class);

	@Autowired
	private PatientIdentificationDao patientIdentificationDao;

	@Override
	public void process(Exchange exchange) throws Exception {
		ORU_R01 message = (ORU_R01) exchange.getIn().getBody();

		@SuppressWarnings("unchecked")
		List<String> deviceIds = (List<String>) exchange.getIn().getHeader(
				DeviceIdScannerProcessor.DEVICE_ID_LIST_HEADER);
		String patientId = findPatientId(deviceIds);

		if (patientId != null) {
			logger.debug("Detected patient ID \"" + patientId + "\"");
			setPatientId(patientId, message);
		}
	}

	private String findPatientId(List<String> deviceIds) {
		if (deviceIds != null) {
			for (String deviceId : deviceIds) {
				String patientId = patientIdentificationDao.findAssociatedPatientId(deviceId);
				if (!StringUtils.isBlank(patientId)) {
					return trimPatientId(patientId);
				}
			}
		}
		return null;
	}
	
	private String trimPatientId(String patientId) {
		if(patientId.startsWith(PATIENT_RESOURCE_PREFIX)) {
			return patientId.replaceFirst(PATIENT_RESOURCE_PREFIX, "");
		} else {
			return patientId;
		}
	}

	private void setPatientId(String patientId, ORU_R01 message) throws HL7Exception {
		PID pidSegment = message.getPATIENT_RESULT().getPATIENT().getPID();
		int numPidReps = pidSegment.getPid3_PatientIdentifierListReps();
		pidSegment.insertPid3_PatientIdentifierList(numPidReps).getCx1_IDNumber().setValue(patientId);
	}
}
