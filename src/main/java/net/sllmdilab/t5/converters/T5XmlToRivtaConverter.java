package net.sllmdilab.t5.converters;

import java.math.BigDecimal;

import net.sll_mdilab.t5.Observation;

import org.springframework.stereotype.Component;

import se.riv.clinicalprocess.healthcond.basic.v1.CVType;
import se.riv.clinicalprocess.healthcond.basic.v1.ObservationType;
import se.riv.clinicalprocess.healthcond.basic.v1.PQType;
import se.riv.clinicalprocess.healthcond.basic.v1.ValueANYType;

@Component
public class T5XmlToRivtaConverter {

	public ObservationType getObservation(Observation t5Obs) {
		ObservationType rivtaObs = new ObservationType();
		
		String obsTypeCode = t5Obs.getObsIdentifier().get(0).getValue();
		CVType type = new CVType();
		type.setCode(obsTypeCode);
		rivtaObs.setType(type);
		
		String obsValue = t5Obs.getValue().get(0).getValue();
		String obsUnit = t5Obs.getUnit().getValue();
		//TODO: check if numeric
		
		ValueANYType value = new ValueANYType();
		PQType pqType = new PQType();
		pqType.setValue(new BigDecimal(obsValue));
		pqType.setUnit(obsUnit);
		value.setPq(pqType);
		rivtaObs.setValue(value);
		
		String obsTimestamp = t5Obs.getTimestamp().getValue().toXMLFormat();
		rivtaObs.setRegistrationTime(obsTimestamp);
		
		return rivtaObs;
	}
}
