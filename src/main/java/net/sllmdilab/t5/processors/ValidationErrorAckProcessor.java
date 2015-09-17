package net.sllmdilab.t5.processors;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.message.ACK;
import ca.uhn.hl7v2.model.v26.segment.ERR;

@Component
public class ValidationErrorAckProcessor implements Processor {
	
	private static Logger logger = LoggerFactory
			.getLogger(ValidationErrorAckProcessor.class);
	
	private static final String ERROR_ID_DEVICE_ID = "0";
	private static final String ERROR_ID_INTERNAL_ERROR = "207";
	private static final String ERROR_CODING_SYSTEM = "HL70357";
	private static final String TEXT_INTERNAL_ERROR = "Application internal error";
	private static final String TEXT_DEVICE_ID = "Device ID";
	private static final String SEVERITY_ERROR = "E";
	private static final String SEVERITY_INFORMATION = "I";
	
	@SuppressWarnings("unchecked")
	@Override
	public void process(Exchange exchange) throws HL7Exception {
		ACK ack = exchange.getIn().getBody(ACK.class);

		List<HL7Exception> exceptions = (List<HL7Exception>) exchange.getIn()
				.getHeader(ProfileValidationProcessor.VALIDATION_ERRORS_HEADER);

		if (exceptions == null) {
			throw new RuntimeException("Missing validation errors header in exchange");
		}
		
		List<String> deviceIds = (List<String>) exchange.getIn().getHeader(ProfileValidationProcessor.DEVICE_ID_LIST_HEADER);
		
		if(deviceIds == null) {
			throw new RuntimeException("Missing deviceIds errors header in exchange");
		}
		
		clearErrSegments(ack);

		populateErrorSegments(ack, exceptions, deviceIds);
	}

	private void populateErrorSegments(ACK ack, List<HL7Exception> exceptions, List<String> deviceIds)
			throws DataTypeException, HL7Exception {
		for (HL7Exception exception : exceptions) {
			logger.debug("Inserting ERR-segment in ACK message: " + exception.getMessage());

			populateErrSegment(exception, ack.insertERR(ack.getERRReps()));
		}
		
		for(String deviceId : deviceIds) {
			populateDeviceInfoErrSegment(deviceId, ack.insertERR(ack.getERRReps()));
		}
	}

	private void clearErrSegments(ACK ack) throws HL7Exception {
		for(int i = 0; i < ack.getERRReps(); ++i) {
			ack.removeERR(0);
		}
	}

	private void populateErrSegment(HL7Exception exception, ERR err)
			throws DataTypeException {
		CWE errorCode = err.getErr3_HL7ErrorCode();
		
		errorCode.getCwe1_Identifier().setValue(ERROR_ID_INTERNAL_ERROR);
		errorCode.getCwe2_Text().setValue(TEXT_INTERNAL_ERROR);
		errorCode.getCwe3_NameOfCodingSystem().setValue(ERROR_CODING_SYSTEM);
		
		err.getErr4_Severity().setValue(SEVERITY_ERROR);
		err.getErr7_DiagnosticInformation()
				.setValue(exception.getMessage());
	}
	
	private void populateDeviceInfoErrSegment(String deviceId, ERR err)
			throws DataTypeException {
		CWE errorCode = err.getErr3_HL7ErrorCode();
		errorCode.getCwe1_Identifier().setValue(ERROR_ID_DEVICE_ID);
		errorCode.getCwe2_Text().setValue(TEXT_DEVICE_ID);
		errorCode.getCwe3_NameOfCodingSystem().setValue(ERROR_CODING_SYSTEM);
		
		err.getErr4_Severity().setValue(SEVERITY_INFORMATION);
		
		err.getErr7_DiagnosticInformation()
				.setValue(deviceId);
	}
}
