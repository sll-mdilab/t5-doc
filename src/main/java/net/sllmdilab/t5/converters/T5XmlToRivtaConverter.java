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
		rivtaObs.setType(getType(obsTypeCode));
		
		String obsValue = t5Obs.getValue().get(0).getValue();
		String obsUnit = t5Obs.getUnit().getValue();
		
		rivtaObs.setValue(getValue(obsValue, obsUnit));
		
		String obsTimestamp = t5Obs.getTimestamp().getValue().toXMLFormat();
		rivtaObs.setRegistrationTime(obsTimestamp);
		
		return rivtaObs;
	}

	private CVType getType(String obsTypeCode) {
		CVType type = new CVType();
		type.setCode(obsTypeCode);
		return type;
	}

	private ValueANYType getValue(String obsValue, String obsUnit) {
		ValueANYType value = new ValueANYType();
		if(isNumeric(obsValue)) {
			PQType pqValue = new PQType();
			pqValue.setValue(new BigDecimal(obsValue));
			pqValue.setUnit(obsUnit);
			value.setPq(pqValue);
		} else {
			CVType cvValue = getType(obsValue);
			value.setCv(cvValue);
		}
		return value;
	}
	
	private boolean isNumeric(String string) {
		try {
			Double.parseDouble(string);
		} catch (NumberFormatException e) {
			return false;
		}
		
		return true;
	}
}
