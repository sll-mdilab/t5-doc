package net.sllmdilab.t5.processors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import net.sllmdilab.t5.dao.PatientIdentificationDao;
import net.sllmdilab.t5.processors.DeviceIdScannerProcessor;
import net.sllmdilab.t5.processors.PatientIdentificationProcessor;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.util.Terser;

@RunWith(MockitoJUnitRunner.class)
public class PatientIdentificationProcessorTest {

	private static final String MOCK_DEVICE_ID = "mock_device_id";

	private static final String MOCK_PATIENT_ID = "mock_patient_id";

	private static final String hl7Input = "MSH|^~\\&|SENDING_APP||||20150303151435||ORU^R01|371357456456|P|2.3||||||8859/1|\r"+
			"PID|\r"+
			"PV1||I|Monitor|\r"+
			"OBR|||||||20150303151435|||Monitor|||58|\r"+
			"OBX|1|NM|1^MDC_ECG_HEART_RATE^MDC||80.000000|1^MDC_DIM_BEAT_PER_MIN^MDC|||||F|||20150303151435|";
	
	@Mock
	private PatientIdentificationDao mockPatientIdentificationDao;
	
	@InjectMocks
	private PatientIdentificationProcessor patientIdentificationProcessor;
	
	private CamelContext camelContext;
	private HapiContext hapiContext;
	
	@Before
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		camelContext = new DefaultCamelContext();
		ModelClassFactory modelClassFactory = new CanonicalModelClassFactory("2.6");
		hapiContext = new DefaultHapiContext(modelClassFactory);
		
	}
	
	@Test
	public void patientIdIsInserted() throws Exception {
		when(mockPatientIdentificationDao.findAssociatedPatientId(any())).thenReturn(MOCK_PATIENT_ID);
		
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		
		ORU_R01 oruMessage = (ORU_R01) hapiContext.getGenericParser().parse(hl7Input);
		message.setBody(oruMessage);
		message.setHeader(
				DeviceIdScannerProcessor.DEVICE_ID_LIST_HEADER, Arrays.asList(MOCK_DEVICE_ID));
		exchange.setIn(message);
		
		patientIdentificationProcessor.process(exchange);
		
		Terser terser = new Terser(oruMessage);
		assertEquals(MOCK_PATIENT_ID, terser.get("/.PID-3"));
	}

}
