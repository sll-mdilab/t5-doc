package net.sllmdilab.t5.processors;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import net.sll_mdilab.t5.Observation;
import net.sllmdilab.t5.converters.T5XmlToRivtaConverter;
import net.sllmdilab.t5.dao.PCD01MessageDao;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import se.riv.clinicalprocess.healthcond.basic.getobservationsresponder.v1.GetObservationsResponseType;
import se.riv.clinicalprocess.healthcond.basic.getobservationsresponder.v1.GetObservationsType;
import se.riv.clinicalprocess.healthcond.basic.v1.IIType;
import se.riv.clinicalprocess.healthcond.basic.v1.ObservationGroupType;
import se.riv.clinicalprocess.healthcond.basic.v1.ObservationType;
import se.riv.clinicalprocess.healthcond.basic.v1.PatientType;

@Component
public class RivtaGetObservationsProcessor implements Processor {
	private static Logger logger = LoggerFactory.getLogger(DeviceIdScannerProcessor.class);

	@Autowired
	private PCD01MessageDao messageDao;

	@Autowired
	private T5XmlToRivtaConverter t5XmlToRivtaConverter;

	private int compareDateStrings(String timestamp1, String timpestamp2) {
		return DatatypeConverter.parseDateTime(timestamp1).compareTo(DatatypeConverter.parseDateTime(timpestamp2));
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		GetObservationsType request = (GetObservationsType) exchange.getIn().getBody();

		String patientId = request.getPatientId().getRoot();
		String observationTypeCode = request.getObservationType().get(0).getCode();
		Date startDateTime = DatatypeConverter.parseDateTime(request.getTime().getStart()).getTime();
		Date endDateTime = DatatypeConverter.parseDateTime(request.getTime().getEnd()).getTime();

		logger.info("Request params: " + patientId + ", " + observationTypeCode + ", " + startDateTime + ", "
				+ endDateTime);

		List<Observation> observations = messageDao.findByPatientIdTimeAndCode(patientId, startDateTime, endDateTime,
				observationTypeCode);

		List<ObservationType> rivtaObservations = observations.stream()
				.map(observation -> t5XmlToRivtaConverter.getObservation(observation))
				.sorted((obs1, obs2) -> compareDateStrings(obs1.getRegistrationTime(), obs2.getRegistrationTime()))
				.collect(Collectors.toList());
		
		exchange.getOut().setBody(createResponse(patientId, rivtaObservations));
	}

	private GetObservationsResponseType createResponse(String patientId, List<ObservationType> rivtaObservations) {
		GetObservationsResponseType response = new GetObservationsResponseType();

		PatientType patient = createPatient(patientId);
		ObservationGroupType observationGroup = new ObservationGroupType();
		observationGroup.setPatient(patient);

		observationGroup.getObservation().addAll(rivtaObservations);
		response.getObservationGroup().add(observationGroup);
		return response;
	}

	private PatientType createPatient(String patientId) {
		PatientType patient = new PatientType();
		IIType patientIdType = new IIType();
		patientIdType.setRoot(patientId);
		patient.setId(patientIdType);
		
		return patient;
	}
}
