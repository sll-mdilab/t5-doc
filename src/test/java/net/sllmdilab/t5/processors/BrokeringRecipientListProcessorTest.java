package net.sllmdilab.t5.processors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import net.sllmdilab.t5.dao.BrokeringReceiverDao;
import net.sllmdilab.t5.domain.BrokeringReceiver;

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

@RunWith(MockitoJUnitRunner.class)
public class BrokeringRecipientListProcessorTest {

	private static final String MOCK_ADDRESS = "mock_address";
	private static final int MOCK_PORT = 4242;
	private static final String MOCK_DEVICE_ID = "mock_device_id";
	private static final String hl7Input = "MSH|^~\\&|SENDING_APP||||20150303151435||ORU^R01|371357456456|P|2.3||||||8859/1|\r"+
			"PID|\r"+
			"PV1||I|Monitor|\r"+
			"OBR|||||||20150303151435|||Monitor|||58|\r"+
			"OBX|1|NM|1^MDC_ECG_HEART_RATE^MDC||80.000000|1^MDC_DIM_BEAT_PER_MIN^MDC|||||F|||20150303151435||||" + MOCK_DEVICE_ID + "|";
	
	@Mock
	private BrokeringReceiverDao brokeringReceiverDao;
	
	@InjectMocks
	private BrokeringRecipientListProcessor brokeringRecipientProcessor;
	
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
	public void patientIdIsSet() throws Exception {
		BrokeringReceiver brokeringReceiver = new BrokeringReceiver();
		brokeringReceiver.setAddress(MOCK_ADDRESS);
		brokeringReceiver.setPort(MOCK_PORT);
		
		when(brokeringReceiverDao.findAllActiveForDeviceId(any())).thenReturn(Arrays.asList(brokeringReceiver));
		
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		
		message.setHeader(DeviceIdScannerProcessor.DEVICE_ID_LIST_HEADER, Arrays.asList(MOCK_DEVICE_ID));
		ORU_R01 oruMessage = (ORU_R01) hapiContext.getGenericParser().parse(hl7Input);
		message.setBody(oruMessage);
		exchange.setIn(message);
		
		brokeringRecipientProcessor.process(exchange);
		
		verify(brokeringReceiverDao).findAllActiveForDeviceId(MOCK_DEVICE_ID);
		
		@SuppressWarnings("unchecked")
		List<String> recipients = (List<String>) message.getHeader(BrokeringRecipientListProcessor.RECIPIENT_LIST_HEADER);
		
		assertTrue(recipients.get(0).contains(MOCK_ADDRESS + ":" + MOCK_PORT));
	}
}
