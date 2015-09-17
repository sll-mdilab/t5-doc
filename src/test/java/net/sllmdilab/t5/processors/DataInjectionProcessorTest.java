package net.sllmdilab.t5.processors;

import static org.junit.Assert.assertEquals;
import net.sllmdilab.t5.processors.DataInjectionProcessor;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.util.Terser;

public class DataInjectionProcessorTest {
	
	private static final String hl7Input = "MSH|^~\\&|SENDING_APP||||20150303151435||ORU^R01|371357456456|P|2.3||||||8859/1|\r"+
			"PID|\r"+
			"PV1||I|Monitor|\r"+
			"OBR|||||||20150303151435|||Monitor|||58|\r"+
			"OBX|1|NM|1^MDC_ECG_HEART_RATE^MDC||80.000000|1^MDC_DIM_BEAT_PER_MIN^MDC|||||F|||20150303151435|";
	
	private DataInjectionProcessor dataInjectionProcessor;
	
	private CamelContext camelContext;
	private HapiContext hapiContext;
	
	@Before
	public void init() throws Exception {
		camelContext = new DefaultCamelContext();
		ModelClassFactory modelClassFactory = new CanonicalModelClassFactory("2.6");
		hapiContext = new DefaultHapiContext(modelClassFactory);
		
		dataInjectionProcessor = new DataInjectionProcessor();
	}

	@Ignore
	@Test
	public void patientIdIsSet() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		
		ORU_R01 oruMessage = (ORU_R01) hapiContext.getGenericParser().parse(hl7Input);
		message.setBody(oruMessage);
		exchange.setIn(message);
		dataInjectionProcessor.process(exchange);
		
		Terser terser = new Terser(oruMessage);
		assertEquals(DataInjectionProcessor.DEFAULT_PATIENT_ID, terser.get("/.PID-3"));
		assertEquals("1.1.1.1", terser.get("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION(0)/OBX-4"));
	}
}
