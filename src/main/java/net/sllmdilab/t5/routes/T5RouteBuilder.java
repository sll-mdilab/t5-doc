package net.sllmdilab.t5.routes;

import static org.apache.camel.component.hl7.HL7.ack;
import net.sllmdilab.t5.processors.BrokeringRecipientListProcessor;
import net.sllmdilab.t5.processors.ProfileValidationProcessor;
import net.sllmdilab.t5.processors.WaveformScannerProcessor;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7DataFormat;
import org.apache.camel.dataformat.soap.SoapJaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.HL7Exception;

@Component
public class T5RouteBuilder extends RouteBuilder {
	public static final String DATABASE_GRAPH_NAME = "http://sll-mdilab.net/T5#graph";

	@Value("${T5_DATABASE_USER}")
	private String databaseUser;

	@Value("${T5_DATABASE_PASSWORD}")
	private String databasePassword;

	@Value("${T5_DATABASE_HOST}")
	private String databaseHost;

	@Value("${T5_DATABASE_PORT}")
	private String databasePort;

	@Value("${T5_RDF_USER}")
	private String rdfUser;

	@Value("${T5_RDF_PASSWORD}")
	private String rdfPassword;

	@Value("${T5_RDF_HOST}")
	private String rdfHost;

	@Value("${T5_RDF_PORT}")
	private String rdfPort;

	@Value("${T5_RDF_GRAPH}")
	private String rdfGraph;

	@Autowired
	private HL7DataFormat hl7DataFormat;
	
	@Autowired
	@Qualifier("rivtaObservationsDataFormat")
	private SoapJaxbDataFormat rivtaObservationsDataFormat;

	@Override
	public void configure() throws Exception {
		//@formatter:off

		// Main route, consumes HL7 messages through MLLP 
		from("netty4:tcp://0.0.0.0:8870?sync=true&encoder=#hl7encoder&decoder=#hl7decoder")
			.routeId("mainRoute")
			.log(LoggingLevel.INFO, "Processing new message.")
			.onException(HL7Exception.class)
				.handled(true)
				.log(LoggingLevel.INFO, "Exception caught, responding with Application Reject ACK.")
				.transform(ack(AcknowledgmentCode.AR))
				.inOnly("seda:ack")
				.stop()
				.end()
			.onException(Exception.class)
				.handled(true)
				.log(LoggingLevel.INFO, "Exception caught, Responding with Application Error ACK.")
				.transform(ack(AcknowledgmentCode.AE))
				.inOnly("seda:ack")
				.stop()
				.end()
			.unmarshal(hl7DataFormat)
			.log(LoggingLevel.INFO, "Performing data injection.")
			.processRef("dataInjectionProcessor")			
			.log(LoggingLevel.INFO, "Extracting device IDs.")
			.processRef("deviceIdScannerProcessor")
			.inOnly("seda:hl7Brokering")
			.log(LoggingLevel.INFO, "Scanning for waveform message.")
			.processRef("waveformScannerProcessor")
			.log(LoggingLevel.INFO, "Performing validation.")
			.processRef("profileValidationProcessor")
			.choice()
				.when(header(ProfileValidationProcessor.VALIDATION_ERRORS_HEADER).isNotNull())
				    .log(LoggingLevel.INFO, "Message contained validation errors.")
				    .inOnly("seda:standardxml")
					.inOnly("seda:t5xml")
					.transform(ack(AcknowledgmentCode.AA))
					.processRef("validationErrorAckProcessor")
					.inOnly("seda:ack")
					.endChoice()
				.otherwise()
					.log(LoggingLevel.INFO, "No validation errors detected.")
					.inOnly("seda:standardxml")
					.inOnly("seda:t5xml")
					.transform(ack())
					.inOnly("seda:ack");
		
		// Convert and persist an ORU_R01 message as HL7 Standard XML
		from("seda:standardxml") 
			.routeId("standardXMLRoute")
			.log(LoggingLevel.INFO, "Converting to HL7v2 Standard-XML.")
			.processRef("standardHL7XMLProcessor")
			.log(LoggingLevel.INFO, "Saving Standard-XML to DB.")
			.to("mldb:standardxml");
		
		// Convert and persist an ACK-message as HL7 Standard XML
		from("seda:ack") 
			.routeId("ackStandardXMLRoute")
			.log(LoggingLevel.INFO, "Converting ACK to HL7v2 Standard-XML.")
			.processRef("standardHL7XMLProcessor")
			.log(LoggingLevel.INFO, "Saving ACK Standard-XML to DB.")
			.inOnly("seda:triples")
			.inOnly("mldb:ack");
		
		// Convert and persist an ORU_R01 message as T5-XML
		from("seda:t5xml") 
			.routeId("t5XMLRoute")
			.log(LoggingLevel.INFO, "Converting to T5-XML.")
			.processRef("t5XMLProcessor")
			.log(LoggingLevel.INFO, "T5-XML to DB.")
			.inOnly("seda:triples")
			.inOnly("mldb:t5xml");
		
		// Convert and persist triples
		from("seda:triples?concurrentConsumers=4") 
			.routeId("triplificationRoute")
			// Ignore waveform messages
			.filter(header(WaveformScannerProcessor.IS_WAVEFORM_HEADER).isNotEqualTo(true))
			.log(LoggingLevel.INFO, "Converting to triples.")
			.processRef("triplificationProcessor")
			.log(LoggingLevel.INFO, "Saving triples to DB.")
			.inOnly("virtuoso://" + rdfUser + ":" + rdfPassword + "@" + rdfHost +":" + rdfPort + "/" + rdfGraph)
			.log(LoggingLevel.INFO, "Finished saving triples to DB.");
		
		// Pass message to defined recipients
		from("seda:hl7Brokering")
			.routeId("hl7BrokeringRoute")
			.log(LoggingLevel.INFO, "Performing brokering.")
			.processRef("patientIdentificationProcessor")
			.processRef("brokeringRecipientListProcessor")
			.marshal(hl7DataFormat)
			.errorHandler(deadLetterChannel("seda:hl7DeadLetter")
				.retryAttemptedLogLevel(LoggingLevel.INFO))
			.recipientList(header(BrokeringRecipientListProcessor.RECIPIENT_LIST_HEADER))
				.parallelProcessing()
				.timeout(2000);
		
		// Handle undeliverable messages
		from("seda:hl7DeadLetter")
			.routeId("hl7DeadLetterRoute")
			.log(LoggingLevel.INFO, "Unable to deliver HL7v2 message.")
			.to("log:hl7DeadLetter?level=INFO");
		
		from("jetty:http://0.0.0.0:8686/clinicalprocess/healthcond/basic/GetObservations/1/rivtabp21?enableJmx=true")
				.log(LoggingLevel.INFO, "Got SOAP request.")
				.unmarshal(rivtaObservationsDataFormat)
				.processRef("rivtaGetObservationsProcessor")
				.marshal(rivtaObservationsDataFormat);
		//@formatter:on
		
	}
}
