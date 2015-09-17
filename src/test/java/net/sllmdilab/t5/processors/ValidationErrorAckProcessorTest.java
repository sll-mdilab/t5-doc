package net.sllmdilab.t5.processors;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sllmdilab.t5.processors.ProfileValidationProcessor;
import net.sllmdilab.t5.processors.ValidationErrorAckProcessor;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Before;
import org.junit.Test;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.conf.check.ProfileNotFollowedException;
import ca.uhn.hl7v2.model.v26.message.ACK;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.util.Terser;

public class ValidationErrorAckProcessorTest {

	private static final String MOCK_DEVICE = "device1";

	private static final String VALIDATION_ERROR_TEXT = "validation_error_text";

	private static final String hl7Input = "MSH|^~\\&|SENDING_APP||||20150303151435||ORU^R01|5846476846454|P|2.3||||||8859/1|\r"+
			"PID|\r"+
			"PV1||I|Monitor|\r"+
			"OBR|||||||20150303151435|||Monitor|||58|\r"+
			"OBX|1|NM|1^MDC_ECG_HEART_RATE^MDC||80.000000|1^MDC_DIM_BEAT_PER_MIN^MDC|||||F|||20150303151435|";
	
	private List<String> deviceIds = Arrays.asList(MOCK_DEVICE);
	
	private ValidationErrorAckProcessor validationErrorAckProcessor;
	
	private CamelContext camelContext;
	private HapiContext hapiContext;
	
	@Before
	public void init() throws Exception {
		camelContext = new DefaultCamelContext();
		ModelClassFactory modelClassFactory = new CanonicalModelClassFactory("2.6");
		hapiContext = new DefaultHapiContext(modelClassFactory);
		validationErrorAckProcessor = new ValidationErrorAckProcessor();
	}

	@Test
	public void errSegmentsAreInjected() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Message ackMessage = new DefaultMessage();
		ORU_R01 oruMessage = (ORU_R01) hapiContext.getGenericParser().parse(hl7Input);
		
		List<HL7Exception> validationErrors = new ArrayList<HL7Exception>();
		validationErrors.add(new ProfileNotFollowedException(VALIDATION_ERROR_TEXT));
		
		ACK ack = (ACK) oruMessage.generateACK(AcknowledgmentCode.AR, new ProfileNotFollowedException("desc"));
		ackMessage.setBody(ack);
		exchange.setIn(ackMessage);
		ackMessage.setHeader(ProfileValidationProcessor.VALIDATION_ERRORS_HEADER, validationErrors);
		ackMessage.setHeader(ProfileValidationProcessor.DEVICE_ID_LIST_HEADER, deviceIds);
		
		validationErrorAckProcessor.process(exchange);
		
		Terser terser = new Terser(ack);
		assertEquals(VALIDATION_ERROR_TEXT, terser.get("/ERR(0)-7"));
		assertEquals(MOCK_DEVICE, terser.get("/ERR(1)-7"));
	}
}
