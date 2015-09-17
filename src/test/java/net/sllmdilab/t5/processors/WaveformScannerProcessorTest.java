package net.sllmdilab.t5.processors;

import static org.junit.Assert.*;
import net.sllmdilab.t5.processors.WaveformScannerProcessor;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Before;
import org.junit.Test;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;

public class WaveformScannerProcessorTest {

	private static final String hl7WaveformInput = "MSH|^~\\&|SendingApplication|SendingFacility|ReceivingApplication|ReceivingFacility|20120530112345||ORU^R01^ORU_R01|9879790004|P|2.6|||NE|AL|USA|ASCII|EN^English^ISO639||IHE_PCD_ORU_R01^IHE PCD^1.3.6.1.4.1.19376.1.6.1.1.1^ISO\r"
			+ "PID|||010101-2425||Doe^John^^^^^B\r"
			+ "PV1||I|ICU^2^23\r"
			+ "OBR|2||XXX|0^WAVEFORM|||20150617120000.000|20150617120001.000\r"
			+ "OBX|1000||69965^MDC_DEV_MON_PHYSIO_MULTI_PARAM_MDS^MDC|1.0.0.0||x^x^x||F|||F|||||||ABC123\r"
			+ "OBX|1001|NA|131329^MDC_ECG_LEAD_I^MDC|1.1.1.1001|-0.53714565^-0.54203324^-0.53128055|266418^MDC_DIM_MILLI_VOLT^MDC|||||F|||20150617120000.000\r"
			+ "OBX|1002|NM|0^MDC_ATTR_SAMP_RATE^MDC|1.1.1.1001.1|150.0|264608^MDC_DIM_PER_SEC|||||F\r"
			+ "OBX|1003|NR|0^MDC_ATTR_DATA_RANGE^MDC|1.1.1.1001.2|-5.0^10.0|266418^MDC_DIM_MILLI_VOLT^MDC|||||F\r"
			+ "OBX|1005|NA|131330^MDC_ECG_LEAD_II^MDC|1.1.1.1002|1.04980469^1.0546875^1.04003906|266418^MDC_DIM_MILLI_VOLT^MDC|||||F|||20150617120000.000\r"
			+ "OBX|1006|NM|0^MDC_ATTR_SAMP_RATE^MDC|1.1.1.1002.1|150.0|264608^MDC_DIM_PER_SEC|||||F\r"
			+ "OBX|1007|NR|0^MDC_ATTR_DATA_RANGE^MDC|1.1.1.1002.2|0.39013672^1.4296875|266418^MDC_DIM_MILLI_VOLT^MDC|||||F\r"
			+ "OBR|3||XXX|0^WAVEFORM|||20150617120000.000|20150617120001.000\r"
			+ "OBX|1013||69965^MDC_DEV_MON_PHYSIO_MULTI_PARAM_MDS^MDC|1.0.0.0||x^x^x||F|||F|||||||ABC123\r"
			+ "OBX|1014|NA|150456^MDC_PULS_OXIM_SAT_O2_WAVEFORM^MDC|1.1.1.1003|1.18^1.17595308^1.17008798|262688^MDC_DIM_PERCENT^MDC|||||F|||20150617120000.000\r"
			+ "OBX|1015|NM|0^MDC_ATTR_SAMP_RATE^MDC|1.1.1.1003.1|125.0|264608^MDC_DIM_PER_SEC|||||F\r"
			+ "OBX|1016|NR|0^MDC_ATTR_DATA_RANGE^MDC|1.1.1.1003.2|0.98924731^3.30107527|262688^MDC_DIM_PERCENT^MDC|||||F";
	
	private static final String hl7Input = "MSH|^~\\&|SENDING_APP||||20150303151435||ORU^R01|371357456456|P|2.3||||||8859/1|\r"+
			"PID|\r"+
			"PV1||I|Monitor|\r"+
			"OBR|||||||20150303151435|||Monitor|||58|\r"+
			"OBX|1|NM|1^MDC_ECG_HEART_RATE^MDC||80.000000|1^MDC_DIM_BEAT_PER_MIN^MDC|||||F|||20150303151435|";
	
	private WaveformScannerProcessor waveformScannerProcessor;
	
	private CamelContext camelContext;
	private HapiContext hapiContext;
	
	@Before
	public void init() throws Exception {
		camelContext = new DefaultCamelContext();
		ModelClassFactory modelClassFactory = new CanonicalModelClassFactory("2.6");
		hapiContext = new DefaultHapiContext(modelClassFactory);
		
		waveformScannerProcessor = new WaveformScannerProcessor();
	}

	@Test
	public void waveformIsDetected() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		
		ORU_R01 oruMessage = (ORU_R01) hapiContext.getGenericParser().parse(hl7WaveformInput);
		message.setBody(oruMessage);
		exchange.setIn(message);
		waveformScannerProcessor.process(exchange);
		
		assertTrue((Boolean)exchange.getIn().getHeader(WaveformScannerProcessor.IS_WAVEFORM_HEADER));
	}
	
	@Test
	public void nonWaveformIsDetected() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		
		ORU_R01 oruMessage = (ORU_R01) hapiContext.getGenericParser().parse(hl7Input);
		message.setBody(oruMessage);
		exchange.setIn(message);
		waveformScannerProcessor.process(exchange);
		
		assertNull(exchange.getIn().getHeader(WaveformScannerProcessor.IS_WAVEFORM_HEADER));
	}
}
