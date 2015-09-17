package net.sllmdilab.t5.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.conf.check.Validator;
import ca.uhn.hl7v2.conf.spec.RuntimeProfile;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;

@Component
public class ProfileValidationProcessor implements Processor {
	private static Logger logger = LoggerFactory.getLogger(ProfileValidationProcessor.class);

	public static final String VALIDATION_ERRORS_HEADER = "validation_errors_header";
	public static final String DEVICE_ID_LIST_HEADER = "device_id_list";
	private static final String[] IGNORED_MESSAGES = {
			"HL7 datatype \\w+ doesn't match profile datatype varies at OBX-5",
			"Required element Text is missing at OBR-4" };

	@Autowired
	private RuntimeProfile runtimeProfile;

	@Autowired
	private Validator validator;

	@Override
	public void process(Exchange exchange) throws Exception {
		if (!(exchange.getIn().getBody() instanceof ORU_R01)) {
			logger.warn("Got non-ORU_R01 message.");
		} else {
			ORU_R01 message = (ORU_R01) exchange.getIn().getBody();

			HL7Exception[] validationExceptions = validator.validate(message, runtimeProfile.getMessage());
			List<HL7Exception> exceptionsOut = filterValidationExceptions(validationExceptions);

			if (!exceptionsOut.isEmpty()) {
				exchange.getIn().setHeader(VALIDATION_ERRORS_HEADER, exceptionsOut);
			}
		}
	}

	private List<HL7Exception> filterValidationExceptions(HL7Exception[] validationExceptions) {
		List<HL7Exception> exceptionsOut = new ArrayList<>();
		for (HL7Exception e : validationExceptions) {
			logger.warn("Validation error: " + e.toString());

			if (!isIgnored(e.getMessage())) {
				exceptionsOut.add(e);
			}
		}
		return exceptionsOut;
	}

	private boolean isIgnored(String errorMessage) {
		for (String ignoredMessageRegex : IGNORED_MESSAGES) {
			if (Pattern.matches(ignoredMessageRegex, errorMessage)) {
				return true;
			}
		}

		return false;
	}
}
