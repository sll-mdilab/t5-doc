package net.sllmdilab.t5.routes;

import static org.junit.Assert.assertEquals;
import net.sllmdilab.t5.config.ApplicationConfiguration;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.message.ACK;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfiguration.class})
@UseAdviceWith
public class RouteTest{
	
	@Autowired
    protected CamelContext camelContext;
	
	@Produce(uri = "mina2:tcp://localhost:8870?sync=true&codec=#hl7codec")
	protected ProducerTemplate minaTemplate;
	
	@EndpointInject(uri = "mock:mldb:standardxml")
	protected MockEndpoint standardHL7XMLEndpoint;
	
	@EndpointInject(uri = "mock:mldb:t5xml")
	protected MockEndpoint t5XMLEndpoint;

	@Before
	public void setUpMocking() throws Exception {
		ModelCamelContext modelCamelContext = (ModelCamelContext)camelContext;
		
		modelCamelContext.getRouteDefinition("mainRoute").adviceWith(modelCamelContext, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				mockEndpoints();
			}
		});
		
		modelCamelContext.getRouteDefinition("standardXMLRoute").adviceWith(modelCamelContext, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				mockEndpoints();
			}
		});
		
		modelCamelContext.getRouteDefinition("t5XMLRoute").adviceWith(modelCamelContext, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				mockEndpoints();
			}
		});
		camelContext.start();
	}
	
	@After
	public void stopContext() throws Exception {
		camelContext.stop();
	}
	
	@Ignore
	@Test
	@DirtiesContext
	public void validHL7IsConvertedAndAckIsSent() throws Exception {
		standardHL7XMLEndpoint.expectedMessageCount(1);
		t5XMLEndpoint.expectedMessageCount(1);
		
		String hl7Input = "MSH|^~\\&|SendingApplication|SendingFacility|ReceivingApplication|ReceivingFacility|20120530112345||ORU^R01^ORU_R01|9879790003|P|2.6|||NE|AL|USA|ASCII|EN^English^ISO639||IHE_PCD_ORU_R01^IHE PCD^1.3.6.1.4.1.19376.1.6.1.1.1^ISO\r"
				+ "PID|||HO2009001^^^NIST^PI||Hon^Albert^^^^^L|Adams^^^^^^L|19610101|M|||15 N Saguaro^^Tucson^AZ^85701\r"
				+ "PV1||I|ICU^2^23\r"
				+ "OBR|1|||44616-1^Pulse oximetry panel ^LN|||20120512031234||||||||||||||||||F|||||||||||||||||||252465000^Pulse oximetry^SCT|7087005^Intermittent^SCT\r"
				+ "NTE|1||This comment refers to all the results in the battery\r"
				+ "OBX|1|NM|8889-8^Heart Rate by Oximetry^LN^149530^ MDC_PULS_OXIM_PULS_RATE^MDC|1.11.2.3|55|{beats}/min^beats per minute^UCUM|35-125||99||R|||20120530112340|||AMEAS^auto-measurement^MDC|0123456789ABCDEF^Pulse_Oximeter_Vendor_X^0123456789ABCDEF^EUI-64||49521004^left ear structure^SCT\n"
				+ "NTE|1||This is a comment about pulse";
		
		Parser parser = new PipeParser();
		ORU_R01 originalMessage = (ORU_R01)parser.parse(hl7Input);
		String encodedHL7 = parser.encode(originalMessage);
		
		String response = minaTemplate.requestBody((Object)encodedHL7, String.class);
		
		ACK responseMessage = (ACK)parser.parse(response);
		
		assertEquals(AcknowledgmentCode.AA.name(), responseMessage.getMSA().getAcknowledgmentCode().getValue());
		
		standardHL7XMLEndpoint.assertIsSatisfied();
		t5XMLEndpoint.assertIsSatisfied();
		
		ORU_R01 outputStandardXMLMessage = (ORU_R01)parseXMLExchange(standardHL7XMLEndpoint.getReceivedExchanges().get(0));
		
		assertEquals(originalMessage.getMSH().getMessageType().getName(), outputStandardXMLMessage.getMSH().getMessageType().getName());
		assertEquals(originalMessage.getMSH().getMessageControlID().getValue(), outputStandardXMLMessage.getMSH().getMessageControlID().getValue());
	}
	
	@Test
	@Ignore
	@DirtiesContext
	public void invalidMessageReutrnsApplicationReject() throws Exception {
		standardHL7XMLEndpoint.expectedMessageCount(1);
		t5XMLEndpoint.expectedMessageCount(1);
		
		String invalidHL7Input = "MSH|^~\\&|SendingApplication|SendingFacility|ReceivingApplication|ReceivingFacility|20120530112345||ORU^R01^ORU_R01|9879790003|P|2.6|||NE|AL|USA|ASCII|EN^English^ISO639||IHE_PCD_ORU_R01^IHE PCD^1.3.6.1.4.1.19376.1.6.1.1.1^ISO\n"
				+ "PID|||HO2009001^^^NIST^PI||Hon^Albert^^^^^L|Adams^^^^^^L|19610101|M|||15 N Saguaro^^Tucson^AZ^85701\n"
				+ "PV1||I|ICU^2^23\n"
				+ "OBR|1|||44616-1^Pulse oximetry panel ^LN|||20120512031234||||||||||||||||||F|||||||||||||||||||252465000^Pulse oximetry^SCT|7087005^Intermittent^SCT\n"
				+ "NTE|1||This comment refers to all the results in the battery\n"
				+ "OBX||||||35-125||99||R|||20120530112340|||AMEAS^auto-measurement^MDC|0123456789ABCDEF^Pulse_Oximeter_Vendor_X^0123456789ABCDEF^EUI-64||49521004^left ear structure^SCT\n"
				+ "NTE|1||This is a comment about pulse";

		String response = minaTemplate.requestBody((Object)invalidHL7Input, String.class);
		
		Parser parser = new PipeParser();
		ACK responseMessage = (ACK)parser.parse(response);
		
		assertEquals(AcknowledgmentCode.AR.name(), responseMessage.getMSA().getAcknowledgmentCode().getValue());
	}

	private Message parseXMLExchange(Exchange exchange) throws HL7Exception {
		String xmlOutput = (String)exchange.getIn().getBody();
		Parser xmlParser = new DefaultXMLParser();

		return xmlParser.parse(xmlOutput);
	}
}