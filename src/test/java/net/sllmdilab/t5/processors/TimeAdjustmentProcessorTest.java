package net.sllmdilab.t5.processors;

import static org.junit.Assert.assertEquals;

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
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v26.datatype.DTM;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;

@RunWith(MockitoJUnitRunner.class)
public class TimeAdjustmentProcessorTest {
	private static final int MOCK_HOURS = 15;
	private static final String MOCK_OFFSET = "0200";
	private static final String MOCK_UTC_OFFSET = "0000";

	//@formatter:off
	private static final String hl7InputNoTimeZone = 
			"MSH|^~\\&|SENDING_APP||||20150303" + MOCK_HOURS + "1435||ORU^R01|371357456456|P|2.3||||||8859/1|\r"+
			"PID|\r"+
			"PV1||I|Monitor|\r"+
			"OBR|||||||20150903151435|||Monitor|||58|\r"+
			"OBX|1|NM|1^MDC_ECG_HEART_RATE^MDC||80.000000|1^MDC_DIM_BEAT_PER_MIN^MDC|||||F|||20150303151435|";
	
	private static final String hl7InputWithTimeZone = 
			"MSH|^~\\&|SENDING_APP||||20150303" + MOCK_HOURS + "1435+" + MOCK_OFFSET +"||ORU^R01|371357456456|P|2.3||||||8859/1|\r"+
			"PID|\r"+
			"PV1||I|Monitor|\r"+
			"OBR|||||||20150903" + MOCK_HOURS +"1435+" + MOCK_OFFSET + "|||Monitor|||58|\r"+
			"OBX|1|NM|1^MDC_ECG_HEART_RATE^MDC||80.000000|1^MDC_DIM_BEAT_PER_MIN^MDC|||||F|||20150303151435|";
	
	private static final String hl7InputWithUtcTimeZone = 
			"MSH|^~\\&|SENDING_APP||||20150303" + MOCK_HOURS + "1435+" + MOCK_UTC_OFFSET +"||ORU^R01|371357456456|P|2.3||||||8859/1|\r"+
			"PID|\r"+
			"PV1||I|Monitor|\r"+
			"OBR|||||||20150903" + MOCK_HOURS +"1435+" + MOCK_UTC_OFFSET + "|||Monitor|||58|\r"+
			"OBX|1|NM|1^MDC_ECG_HEART_RATE^MDC||80.000000|1^MDC_DIM_BEAT_PER_MIN^MDC|||||F|||20150303151435|";
	//@formatter:on
	
	@InjectMocks
	private TimeAdjustmentProcessor timeAdjustmentProcessor = new TimeAdjustmentProcessor(true, "GMT+2:00");
	
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
	public void timeWithoutZoneIsChanged() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		
		ORU_R01 oruMessage = (ORU_R01) hapiContext.getGenericParser().parse(hl7InputNoTimeZone);
		message.setBody(oruMessage);
		exchange.setIn(message);
		
		DTM messageDtm = oruMessage.getMSH().getDateTimeOfMessage();
		
		assertEquals(-99, messageDtm.getGMTOffset());
		assertEquals(MOCK_HOURS, messageDtm.getHour());
		
		timeAdjustmentProcessor.process(exchange);
		messageDtm = oruMessage.getMSH().getDateTimeOfMessage();
		
		assertEquals(200, messageDtm.getGMTOffset());
		assertEquals(MOCK_HOURS, messageDtm.getHour());
		
		DTM obrDtm = oruMessage.getPATIENT_RESULT(0).getORDER_OBSERVATION(0).getOBR().getObr7_ObservationDateTime();
		
		assertEquals(200, obrDtm.getGMTOffset());
		assertEquals(MOCK_HOURS, messageDtm.getHour());
	}
	
	@Test
	public void timeWithZoneIsNotChanged() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		
		ORU_R01 oruMessage = (ORU_R01) hapiContext.getGenericParser().parse(hl7InputWithTimeZone);
		message.setBody(oruMessage);
		exchange.setIn(message);
		
		DTM messageDtm = oruMessage.getMSH().getDateTimeOfMessage();
		
		assertEquals(200, messageDtm.getGMTOffset());
		assertEquals(MOCK_HOURS, messageDtm.getHour());
		
		timeAdjustmentProcessor.process(exchange);
		messageDtm = oruMessage.getMSH().getDateTimeOfMessage();
		
		assertEquals(200, messageDtm.getGMTOffset());
		assertEquals(MOCK_HOURS, messageDtm.getHour());
	}
	
	@Test
	public void timeWithUtcZoneIsNotChanged() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		
		ORU_R01 oruMessage = (ORU_R01) hapiContext.getGenericParser().parse(hl7InputWithUtcTimeZone);
		message.setBody(oruMessage);
		exchange.setIn(message);
		
		DTM messageDtm = oruMessage.getMSH().getDateTimeOfMessage();
		
		assertEquals(0, messageDtm.getGMTOffset());
		assertEquals(MOCK_HOURS, messageDtm.getHour());
		
		timeAdjustmentProcessor.process(exchange);
		messageDtm = oruMessage.getMSH().getDateTimeOfMessage();
		
		assertEquals(0, messageDtm.getGMTOffset());
		assertEquals(MOCK_HOURS, messageDtm.getHour());
	}
}
