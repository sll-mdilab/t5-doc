package net.sllmdilab.t5.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;

@Component
public class WaveformScannerProcessor implements Processor {
	
	private static Logger logger = LoggerFactory
			.getLogger(WaveformScannerProcessor.class);
	
	public static String IS_WAVEFORM_HEADER = "is_waveform_header";
	
	@Override
	public void process(Exchange exchange) throws Exception {
		ORU_R01 message = (ORU_R01) exchange.getIn().getBody(Message.class);
		
		if(universalServiceIdentifierContains("WAVEFORM", message)) {
			logger.info("Waveform message detected.");
			exchange.getIn().setHeader(IS_WAVEFORM_HEADER,  true);
		} else {
			logger.info("Non-waveform message detected");
		}
	}
	
	private boolean universalServiceIdentifierContains(String string, ORU_R01 message) throws HL7Exception {
		for(ORU_R01_PATIENT_RESULT patientResult : message.getPATIENT_RESULTAll()) {
			for(ORU_R01_ORDER_OBSERVATION orderObservation : patientResult.getORDER_OBSERVATIONAll()) {
				String usi = orderObservation.getOBR().getObr4_UniversalServiceIdentifier().getCwe2_Text().getValue();
				if(usi != null && usi.contains(string)) {
					return true;
				}
			}
		}
		return false;
	}
}
