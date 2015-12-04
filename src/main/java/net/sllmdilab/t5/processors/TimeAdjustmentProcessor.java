package net.sllmdilab.t5.processors;

import java.util.Date;
import java.util.TimeZone;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v26.datatype.DTM;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import net.sllmdilab.t5.dao.ObservationDao;

public class TimeAdjustmentProcessor implements Processor {
	private static final Logger logger = LoggerFactory.getLogger(TimeAdjustmentProcessor.class);
	
	private static final int MILLIS_PER_MINUTE = 60 * 1000;
	private static final int MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60;

	private boolean timeAdjustmentEnabled;
	private String timeZoneId;

	public TimeAdjustmentProcessor(boolean timeAdjustmentEnabled, String timeZoneId) {
		this.timeAdjustmentEnabled = timeAdjustmentEnabled;
		this.timeZoneId = timeZoneId;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		if (timeAdjustmentEnabled) {
			logger.info("Adjusting timestamps.");
			
			Message message = exchange.getIn().getBody(Message.class);
			ORU_R01 oruMessage = (ORU_R01) message;

			DTM mshDateTime = oruMessage.getMSH().getMsh7_DateTimeOfMessage();
			adjustDate(mshDateTime);

			adjustObrDates(oruMessage);
		}
	}

	private void adjustObrDates(ORU_R01 oruMessage) throws HL7Exception, DataTypeException {
		for (ORU_R01_PATIENT_RESULT patientResult : oruMessage.getPATIENT_RESULTAll()) {
			for (ORU_R01_ORDER_OBSERVATION orderObservation : patientResult.getORDER_OBSERVATIONAll()) {
				adjustDate(orderObservation.getOBR().getObr7_ObservationDateTime());
				adjustDate(orderObservation.getOBR().getObr8_ObservationEndDateTime());

				adjustObsDates(orderObservation);
			}
		}
	}

	private void adjustObsDates(ORU_R01_ORDER_OBSERVATION orderObservation) throws HL7Exception {
		for (ORU_R01_OBSERVATION observation : orderObservation.getOBSERVATIONAll()) {
			adjustDate(observation.getOBX().getDateTimeOfTheObservation());
			adjustDate(observation.getOBX().getDateTimeOfTheAnalysis());
		}
	}

	private void adjustDate(DTM dtm) throws HL7Exception {
		// Missing offset, assume the provided timezone
		if (!dtm.isEmpty() && dtm.getGMTOffset() == -99) {
			int milliOffset = TimeZone.getTimeZone(timeZoneId).getOffset(new Date().getTime());

			dtm.setOffset(millisOffsetToDtm(milliOffset));
		}
	}

	private int millisOffsetToDtm(long milliOffset) {
		long hourOffset = (milliOffset / MILLIS_PER_HOUR);
		long minuteOffset = (milliOffset % MILLIS_PER_HOUR) / MILLIS_PER_MINUTE;

		return (int) (100 * hourOffset + minuteOffset);
	}
}
