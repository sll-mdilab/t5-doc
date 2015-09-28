package net.sllmdilab.t5.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import net.sll_mdilab.t5.Observation;
import net.sllmdilab.commons.database.MLDBClient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PCD01MessageDaoTest {

	private static final String MOCK_UNIT_CODE = "mock_unit_code";
	private static final String MOCK_PATIENT_ID = "121212121212";
	private static final String MOCK_DEVICE_ID = "mock_device_id";
	private static final String MOCK_OBS_TYPE_CODE = "mock_obs_type_code";
	//@formatter:off
	private static final String MOCK_XML = 
			"<Observation hierarchy=\"1.1.1.2\" index=\"2\" setid=\"2\" uid=\"3ff485e2-411f-48ce-984b-3acf7fed7ec1\">" + 
			"<ObsIdentifier codingSystemName=\"MDC\" id=\"149530\">" + MOCK_OBS_TYPE_CODE  + 
			"</ObsIdentifier>" + 
			"<Value typeHL7v2=\"NM\">91.000000" + 
			"</Value>" + 
			"<Unit codingSystemName=\"MDC\" id=\"264864\">" + MOCK_UNIT_CODE +
			"</Unit>" + 
			"<Timestamp>2015-09-08T07:55:42.000" + 
			"</Timestamp>" + 
			"<EquipmentIdentifier nameSpaceID=\"\">" + MOCK_DEVICE_ID +"</EquipmentIdentifier>" + 
			"</Observation>";
	private static final String MOCK_XML_2 = "<observations xmlns=\"http://sll-mdilab.net/T5/\">" + MOCK_XML + MOCK_XML + "</observations>" ;
	//@formatter:on

	@Mock
	private MLDBClient mldbClient;

	@InjectMocks
	private PCD01MessageDao messageDao;

	@Before
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void listIsParsed() {
		when(mldbClient.sendQuery(any())).thenReturn(MOCK_XML_2);

		Date start = new Date(100000);
		Date end = new Date(101000);

		List<Observation> observations = messageDao.findByPatientIdTimeAndCode(MOCK_PATIENT_ID, start, end,
				MOCK_OBS_TYPE_CODE);
		assertEquals(2, observations.size());
		assertEquals(observations.get(0).getObsIdentifier().get(0).getValue(), MOCK_OBS_TYPE_CODE);
		assertEquals(observations.get(0).getUnit().getValue(), MOCK_UNIT_CODE);
	}
}
