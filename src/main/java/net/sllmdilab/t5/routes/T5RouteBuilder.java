package net.sllmdilab.t5.routes;

import static org.apache.camel.component.hl7.HL7.ack;
import net.sllmdilab.t5.processors.BrokeringRecipientListProcessor;
import net.sllmdilab.t5.processors.ProfileValidationProcessor;
import net.sllmdilab.t5.processors.WaveformScannerProcessor;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hl7.HL7DataFormat;
import org.apache.camel.dataformat.soap.SoapJaxbDataFormat;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.HL7Exception;
import static org.apache.camel.builder.PredicateBuilder.*;

@Component
public class T5RouteBuilder extends RouteBuilder {
	public static final String DATABASE_GRAPH_NAME = "http://sll-mdilab.net/T5#graph";

	@Value("${T5_PORT}")
	private String hl7Port;

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
		from("netty4:tcp://0.0.0.0:" + hl7Port + "?sync=true&encoder=#hl7encoder&decoder=#hl7decoder")
			.routeId("mainRoute")
			.log(LoggingLevel.INFO, "Processing new message.")
			.onException(HL7Exception.class)
				.handled(true)
				.log(LoggingLevel.INFO, "Exception caught, responding with Application Reject ACK.")
				.transform(ack(AcknowledgmentCode.AR))
//				.inOnly("seda:ack")
				.stop()
				.end()
			.onException(Exception.class)
				.handled(true)
				.log(LoggingLevel.INFO, "Exception caught, Responding with Application Error ACK.")
				.transform(ack(AcknowledgmentCode.AE))
//				.inOnly("seda:ack")
				.stop()
				.end()
			.unmarshal(hl7DataFormat)
			.process("timeAdjustmentProcessor")
			.log(LoggingLevel.INFO, "Performing data injection.")
			.process("dataInjectionProcessor")			
			.log(LoggingLevel.INFO, "Extracting device IDs.")
			.process("deviceIdScannerProcessor")
//			.inOnly("seda:hl7Brokering")
			.log(LoggingLevel.INFO, "Scanning for waveform message.")
			.process("waveformScannerProcessor")
			.log(LoggingLevel.INFO, "Performing validation.")
			.process("profileValidationProcessor")
			.choice()
				.when(header(ProfileValidationProcessor.VALIDATION_ERRORS_HEADER).isNotNull())
				    .log(LoggingLevel.INFO, "Message contained validation errors.")
//				    .inOnly("seda:standardxml")
					.inOnly("seda:t5xml")
					.transform(ack(AcknowledgmentCode.AA))
					.process("validationErrorAckProcessor")
//					.inOnly("seda:ack")
					.endChoice()
				.otherwise()
					.log(LoggingLevel.INFO, "No validation errors detected.")
					.inOnly("seda:standardxml")
					.inOnly("seda:t5xml")
					.transform(ack());
//					.inOnly("seda:ack");
		
		// Convert and persist an ORU_R01 message as HL7 Standard XML
//		from("seda:standardxml") 
//			.routeId("standardXMLRoute")
//			.log(LoggingLevel.INFO, "Converting to HL7v2 Standard-XML.")
//			.process("standardHL7XMLProcessor")
//			.log(LoggingLevel.INFO, "Saving Standard-XML to DB.")
//			.to("mldb:standardxml");
		
		// Convert and persist an ACK-message as HL7 Standard XML
//		from("seda:ack") 
//			.routeId("ackStandardXMLRoute")
//			.log(LoggingLevel.INFO, "Converting ACK to HL7v2 Standard-XML.")
//			.process("standardHL7XMLProcessor")
//			.log(LoggingLevel.INFO, "Saving ACK Standard-XML to DB.")
//			.inOnly("seda:triples")
//			.inOnly("mldb:ack");
		
		// Convert and persist an ORU_R01 message as T5-XML
		from("seda:t5xml") 
			.routeId("t5XMLRoute")
			.log(LoggingLevel.INFO, "Converting to T5-XML.")
			.process("t5XMLProcessor")
			.log(LoggingLevel.INFO, "T5-XML to DB.")
			.inOnly("seda:triples")
			.process("sqlProcessor");
		
		// Convert and persist triples
		from("seda:triples?concurrentConsumers=4") 
			.routeId("triplificationRoute")
			// Ignore waveform messages
			.filter(
					and(
						constant(StringUtils.isBlank(rdfHost)).isEqualTo(false), 
						header(WaveformScannerProcessor.IS_WAVEFORM_HEADER).isNotEqualTo(true))
					)
				.log(LoggingLevel.INFO, "Converting to triples.")
				.process("triplificationProcessor")
				.log(LoggingLevel.INFO, "Saving triples to DB.")
				.inOnly("virtuoso://" + rdfUser + ":" + rdfPassword + "@" + rdfHost +":" + rdfPort + "/" + rdfGraph)
				.log(LoggingLevel.INFO, "Finished saving triples to DB.");
		
		// Pass message to defined recipients
		from("seda:hl7Brokering")
			.routeId("hl7BrokeringRoute")
			.log(LoggingLevel.INFO, "Performing brokering.")
			.process("patientIdentificationProcessor")
			.process("brokeringRecipientListProcessor")
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
		
	}
}
