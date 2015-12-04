package net.sllmdilab.t5.converters;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.Parser;
import net.sllmdilab.commons.t5.validators.RosettaValidator;
import net.sllmdilab.t5.util.XMLHelper;

@RunWith(MockitoJUnitRunner.class)
public class PCD_01MessageToXMLConverterTest {
	private static final String MOCK_HARMONIZED_DESC = "MOCK_HARMONIZED_DESC";
	private static final String MOCK_HARMONIZED_UNIT = "MOCK_HARMONIZED_UNIT";

	private static final String hl7Input = "MSH|^~\\&|SendingApplication|SendingFacility|ReceivingApplication|ReceivingFacility|20120530112345||ORU^R01^ORU_R01|9879790003|P|2.6|||NE|AL|USA|ASCII|EN^English^ISO639||IHE_PCD_ORU_R01^IHE PCD^1.3.6.1.4.1.19376.1.6.1.1.1^ISO\r"
			+ "PID|||HO2009001^^^NIST^PI||Hon^Albert^^^^^L|Adams^^^^^^L|19610101|M|||15 N Saguaro^^Tucson^AZ^85701\r"
			+ "PV1||I|ICU^2^23\r"
			+ "OBR|1|||44616-1^Pulse oximetry panel ^LN|||20120512031234||||||||||||||||||F|||||||||||||||||||252465000^Pulse oximetry^SCT|7087005^Intermittent^SCT\r"
			+ "NTE|1||This comment refers to all the results in the battery\r"
			+ "OBX|1|NM|8889-8^Heart Rate by Oximetry^LN^149530^MDC_PULS_OXIM_PULS_RATE^MDC|1.11.2.3|55|{beats}/min^beats per minute^UCUM|35-125||99||R|||20120530112340|||AMEAS^auto-measurement^MDC|0123456789ABCDEF^Pulse_Oximeter_Vendor_X^0123456789ABCDEF^EUI-64||49521004^left ear structure^SCT\r"
			+ "NTE|1||This is a comment about pulse";

	private static final String hl7WaveformInput = "MSH|^~\\&|SendingApplication|SendingFacility|ReceivingApplication|ReceivingFacility|20120530112345||ORU^R01^ORU_R01|9879790004|P|2.6|||NE|AL|USA|ASCII|EN^English^ISO639||IHE_PCD_ORU_R01^IHE PCD^1.3.6.1.4.1.19376.1.6.1.1.1^ISO\r"
			+ "PID|||010101-2425||Doe^John^^^^^B\r"
			+ "PV1||I|ICU^2^23\r"
			+ "OBR|2||XXX|WAVEFORM|||20150617120000.000|20150617120001.000\r"
			+ "OBX|1000||69965^MDC_DEV_MON_PHYSIO_MULTI_PARAM_MDS^MDC|1.0.0.0||x^x^x||F|||F|||||||ABC123\r"
			+ "OBX|1001|NA|131329^MDC_ECG_LEAD_I^MDC|1.1.1.1001|-0.53714565^-0.54203324^-0.53128055|266418^MDC_DIM_MILLI_VOLT^MDC|||||F|||20150617120000.000\r"
			+ "OBX|1002|NM|0^MDC_ATTR_SAMP_RATE^MDC|1.1.1.1001.1|150.0|264608^MDC_DIM_PER_SEC|||||F\r"
			+ "OBX|1003|NR|0^MDC_ATTR_DATA_RANGE^MDC|1.1.1.1001.2|-5.0^10.0|266418^MDC_DIM_MILLI_VOLT^MDC|||||F\r"
			+ "OBX|1005|NA|131330^MDC_ECG_LEAD_II^MDC|1.1.1.1002|1.04980469^1.0546875^1.04003906|266418^MDC_DIM_MILLI_VOLT^MDC|||||F|||20150617120000.000\r"
			+ "OBX|1006|NM|0^MDC_ATTR_SAMP_RATE^MDC|1.1.1.1002.1|150.0|264608^MDC_DIM_PER_SEC|||||F\r"
			+ "OBX|1007|NR|0^MDC_ATTR_DATA_RANGE^MDC|1.1.1.1002.2|0.39013672^1.4296875|266418^MDC_DIM_MILLI_VOLT^MDC|||||F\r"
			+ "OBR|3||XXX|WAVEFORM|||20150617120000.000|20150617120001.000\r"
			+ "OBX|1013||69965^MDC_DEV_MON_PHYSIO_MULTI_PARAM_MDS^MDC|1.0.0.0||x^x^x||F|||F|||||||ABC123\r"
			+ "OBX|1014|NA|150456^MDC_PULS_OXIM_SAT_O2_WAVEFORM^MDC|1.1.1.1003|1.18^1.17595308^1.17008798|262688^MDC_DIM_PERCENT^MDC|||||F|||20150617120000.000\r"
			+ "OBX|1015|NM|0^MDC_ATTR_SAMP_RATE^MDC|1.1.1.1003.1|125.0|264608^MDC_DIM_PER_SEC|||||F\r"
			+ "OBX|1016|NR|0^MDC_ATTR_DATA_RANGE^MDC|1.1.1.1003.2|0.98924731^3.30107527|262688^MDC_DIM_PERCENT^MDC|||||F";

	@Mock
	private RosettaValidator mockRosettaValidator;

	@InjectMocks
	private PCD_01MessageToXMLConverter converter;

	private XPath xPath;

	@Before
	public void init() throws Exception {

		NamespaceContext nsContext = new NamespaceContext() {
		    public String getNamespaceURI(String prefix) {
		    	if(prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
		    		return PCD_01MessageToXMLConverter.T5_XML_NAMESPACE;
		    	}
		    	
		        return prefix.equals("t5") ? PCD_01MessageToXMLConverter.T5_XML_NAMESPACE : null; 
		    }
		    public Iterator getPrefixes(String val) {
		        return null;
		    }
		    public String getPrefix(String uri) {
		        return null;
		    }
		};
		
		XPathFactory xpathfactory = XPathFactory.newInstance();
		xPath = xpathfactory.newXPath();
		xPath.setNamespaceContext(nsContext);

		MockitoAnnotations.initMocks(this);

		when(mockRosettaValidator.getHarmonizedUCUMUnits(any())).thenReturn(MOCK_HARMONIZED_UNIT);
		when(mockRosettaValidator.getHarmonizedUCUMUnits(any())).thenReturn(MOCK_HARMONIZED_DESC);

		when(mockRosettaValidator.isInTermsTable(any())).thenReturn(true);
	}

	@Test
	public void validMessageIsConverted() throws Exception {
		@SuppressWarnings("resource")
		HapiContext context = new DefaultHapiContext();
		context.setModelClassFactory(new DefaultModelClassFactory());
		Parser parser = context.getPipeParser();
		ORU_R01 originalMessage = (ORU_R01) parser.parse(hl7Input);

		Document xmlDocument = converter.getXML(originalMessage);

		XMLHelper.prettyPrintDocument(xmlDocument, System.out);

		assertEquals(
				"1.11.2.3",
				stringXPath(
						"/t5:PCD_01_Message/Patient_Result/Order_Observations/MDS/VMD/CHAN/Metric/Observation/@hierarchy",
						xmlDocument));
	}

	@Test
	public void validWaveformMessageConverted() throws Exception {
		@SuppressWarnings("resource")
		HapiContext context = new DefaultHapiContext();
		context.setModelClassFactory(new DefaultModelClassFactory());
		Parser parser = context.getPipeParser();
		ORU_R01 originalMessage = (ORU_R01) parser.parse(hl7WaveformInput);

		Document xmlDocument = converter.getXML(originalMessage);
		
		XMLHelper.prettyPrintDocument(xmlDocument, System.out);
		NodeList nodes = null; 
	
		nodes = nodeListXPath("/t5:PCD_01_Message/Patient_Result/Order_Observations", xmlDocument);
		assertEquals(2, nodes.getLength());
		
		nodes = nodeListXPath("/t5:PCD_01_Message/Patient_Result/Order_Observations[1]/MDS", xmlDocument);
		assertEquals(1, nodes.getLength());
		
		nodes = nodeListXPath("/t5:PCD_01_Message/Patient_Result/Order_Observations[1]/MDS/VMD/CHAN/Metric", xmlDocument);
		assertEquals(2, nodes.getLength());
		
		nodes = nodeListXPath("/t5:PCD_01_Message/Patient_Result/Order_Observations[1]/MDS/VMD/CHAN/Metric[1]/*", xmlDocument);
		assertEquals(3, nodes.getLength());
		
		nodes = nodeListXPath("/t5:PCD_01_Message/Patient_Result/Order_Observations[1]/MDS/VMD/CHAN/Metric[1]/Facet", xmlDocument);
		assertEquals(2, nodes.getLength());
		
		String obsId = stringXPath("/t5:PCD_01_Message/Patient_Result/Order_Observations[1]/MDS/VMD/CHAN/Metric[1]/Observation[@hierarchy='1.1.1.1001']/ObsIdentifier", xmlDocument);
		assertEquals("MDC_ECG_LEAD_I", obsId);

	}

	private String stringXPath(String xPathString, Document document) throws XPathExpressionException {
		return (String) xPath.evaluate(xPathString, document, XPathConstants.STRING);
	}
	
	private NodeList nodeListXPath(String xPathString, Document document) throws XPathExpressionException {
		return (NodeList) xPath.evaluate(xPathString, document, XPathConstants.NODESET);
	}

}
