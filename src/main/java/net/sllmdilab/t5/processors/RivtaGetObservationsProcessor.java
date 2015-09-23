package net.sllmdilab.t5.processors;

import java.math.BigDecimal;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.riv.clinicalprocess.healthcond.basic.getobservationsresponder.v1.GetObservationsResponseType;
import se.riv.clinicalprocess.healthcond.basic.getobservationsresponder.v1.GetObservationsType;
import se.riv.clinicalprocess.healthcond.basic.v1.CVType;
import se.riv.clinicalprocess.healthcond.basic.v1.IIType;
import se.riv.clinicalprocess.healthcond.basic.v1.ObservationGroupType;
import se.riv.clinicalprocess.healthcond.basic.v1.ObservationType;
import se.riv.clinicalprocess.healthcond.basic.v1.PQType;
import se.riv.clinicalprocess.healthcond.basic.v1.PatientType;
import se.riv.clinicalprocess.healthcond.basic.v1.ValueANYType;

@Component
public class RivtaGetObservationsProcessor implements Processor {
	private static Logger logger = LoggerFactory.getLogger(DeviceIdScannerProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		GetObservationsType in = (GetObservationsType) exchange.getIn().getBody();
		logger.info("Called.");
		
		GetObservationsResponseType response = new GetObservationsResponseType();
		PatientType patient = new PatientType();
		patient.setName("Tolvan Tolvansson");
		
		IIType patientId = new IIType();
		patientId.setRoot("1212121212");
		patient.setId(patientId);
		ObservationGroupType observationGroup = new ObservationGroupType();
		observationGroup.setPatient(patient);
		
		ObservationType observation = new ObservationType();
		CVType type = new CVType();
		type.setCode("MDC_HEARD_BEAT_RATE");
		ValueANYType value = new ValueANYType();
		PQType pqType = new PQType();
		pqType.setValue(new BigDecimal(78.0));
		pqType.setUnit("MDC_BEATS_PER_MINUTE");
		value.setPq(pqType);
		observation.setType(type);
		observation.setValue(value);
		observationGroup.getObservation().add(observation);
		
		response.getObservationGroup().add(observationGroup);
		
		exchange.getOut().setBody(response);
	}
}
