package net.sllmdilab.t5.converters;

import static org.junit.Assert.assertEquals;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import net.sll_mdilab.t5.Observation;
import net.sll_mdilab.t5.Timestamp;
import net.sll_mdilab.t5.Unit;
import net.sll_mdilab.t5.Value;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import se.riv.clinicalprocess.healthcond.basic.v1.ObservationType;

@RunWith(MockitoJUnitRunner.class)
public class T5XmlToRivtaConverterTest {

	private static final String MOCK_TIMESTAMP = "2015-12-12T12:12:12.121";
	private static final String MOCK_UNIT = "mock_unit";
	private static final double MOCK_VALUE = 42.01;
	private T5XmlToRivtaConverter converter;
	
	@Before
	public void begin() {
		converter = new T5XmlToRivtaConverter();
	}
	
	@Test
	public void observationIsConverted() throws Exception {
		Observation obs = new Observation();
		Value value = new Value();
		value.setValue(Double.toString(MOCK_VALUE));
		obs.getValue().add(value);
		
		Unit unit = new Unit();
		unit.setValue(MOCK_UNIT);
		obs.setUnit(unit);
		
		Timestamp timestamp = new Timestamp();
		XMLGregorianCalendar calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(MOCK_TIMESTAMP);
		timestamp.setValue(calendar);
		obs.setTimestamp(timestamp);
		
		ObservationType rivtaObs = converter.getObservation(obs);
		
		assertEquals(MOCK_VALUE, rivtaObs.getValue().getPq().getValue().doubleValue(), 0.0001);
		assertEquals(MOCK_UNIT, rivtaObs.getValue().getPq().getUnit());
		assertEquals(MOCK_TIMESTAMP, rivtaObs.getRegistrationTime());
	}
}
