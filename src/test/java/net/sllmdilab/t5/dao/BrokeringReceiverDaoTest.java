package net.sllmdilab.t5.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import net.sllmdilab.commons.database.MLDBClient;
import net.sllmdilab.t5.domain.BrokeringReceiver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BrokeringReceiverDaoTest {
	private static final String MOCK_ADDRESS = "mock_address";
	private static final int MOCK_PORT = 5648;
	private static final String MOCK_RECEIVERS_XML = "<receiverList><receiver><address>" + MOCK_ADDRESS
			+ "</address><port>" + MOCK_PORT + "</port></receiver></receiverList>";

	@Mock
	private MLDBClient mldbClient;

	@InjectMocks
	private BrokeringReceiverDao receiverDao;

	@Before
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void listIsParsed() {
		when(mldbClient.sendQuery(any())).thenReturn(MOCK_RECEIVERS_XML);

		List<BrokeringReceiver> subscriptions = receiverDao.findAllActiveForDeviceId("device001");
		assertEquals(1, subscriptions.size());
		BrokeringReceiver receiver = subscriptions.get(0);
		assertEquals(MOCK_ADDRESS, receiver.getAddress());
		assertEquals(MOCK_PORT, receiver.getPort());
	}
}
