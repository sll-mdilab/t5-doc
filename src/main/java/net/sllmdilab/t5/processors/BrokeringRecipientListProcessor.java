package net.sllmdilab.t5.processors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sllmdilab.t5.dao.BrokeringReceiverDao;
import net.sllmdilab.t5.domain.BrokeringReceiver;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrokeringRecipientListProcessor implements Processor {
	private static Logger logger = LoggerFactory.getLogger(BrokeringRecipientListProcessor.class);

	@Autowired
	private BrokeringReceiverDao brokeringReceiverDao;

	public static final String RECIPIENT_LIST_HEADER = "recipient_list_header";

	@Override
	public void process(Exchange exchange) throws Exception {
		@SuppressWarnings("unchecked")
		List<String> deviceIds = (List<String>) exchange.getIn().getHeader(
				DeviceIdScannerProcessor.DEVICE_ID_LIST_HEADER);

		if (deviceIds == null || deviceIds.isEmpty()) {
			logger.debug("No Device ID found, skipping message brokering.");
		}

		long beforeQueryMillis = System.currentTimeMillis();
		Set<String> endpoints = new HashSet<>();
		for (String deviceId : deviceIds) {
			for (BrokeringReceiver receiver : brokeringReceiverDao.findAllActiveForDeviceId(deviceId)) {
				String endpoint = "netty4:tcp://" + receiver.getAddress() + ":" + receiver.getPort()
						+ "?sync=true&encoder=#hl7encoder&decoder=#hl7decoder";
				logger.debug("Adding brokering recipient: " + endpoint);
				endpoints.add(endpoint);
			}
		}

		logger.debug("Resolving brokering receivers took " + (System.currentTimeMillis() - beforeQueryMillis) + " ms.");

		exchange.getIn().setHeader(RECIPIENT_LIST_HEADER, new ArrayList<String>(endpoints));
	}
}
