package net.sllmdilab.t5.converters;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import net.sllmdilab.commons.domain.SqlObservation;
import net.sllmdilab.commons.util.ParserUtils;

@RunWith(MockitoJUnitRunner.class)
public class PCD_01MessageToSqlObservationsConverterTest {
	
private static final String MOCK_VMD_DEVICE_ID = "MOCK_VMD_DEVICE_ID";


private static final String MOCK_MDS_DEVICE_ID = "DEVICE_ID";


private static final String MOCK_MESSAGE = 
		"<PCD_01_Message xmlns=\"http://sll-mdilab.net/T5/\" id=\"HK453HJH5K3546\" timeStamp=\"2015-10-07T13:23:08.000\">\n" + 
		"  <Sending_Application idLocal=\"SENDING_APP\"/>\n" + 
		"  <Sending_Facility idLocal=\"\" idUniversal=\"\" idUniversalType=\"\"/>\n" + 
		"  <Patient_Result>\n" + 
		"    <Patient>\n" + 
		"      <Identifier authority=\"\" typeCode=\"MR\">121212-1212</Identifier>\n" + 
		"    </Patient>\n" + 
		"    <Order_Observations>\n" + 
		"      <Order timeStamp=\"2015-10-07T13:23:01.000\">\n" + 
		"  <FillerOrderNumber/>\n" + 
		"  <UniversalServiceID codingSystemName=\"MDC\" id=\"69965\">MDC_DEV_MON_PHYSIO_MULTI_PARAM_MDS</UniversalServiceID>\n" + 
		"      </Order>\n" + 
		"      <MDS id=\"MDC_DEV_MON_PHYSIO_MULTI_PARAM_MDS\" index=\"1\">\n" + 
		"  <Observation hierarchy=\"1.0.0.0\" index=\"0\" setid=\"\" uid=\"cb03a343-7c6d-46b7-a7bc-f630baad72d5\">\n" + 
		"    <ObsIdentifier codingSystemName=\"MDC\" id=\"69965\" notFound=\"true\">MDC_DEV_MON_PHYSIO_MULTI_PARAM_MDS</ObsIdentifier>\n" + 
		"    <Timestamp>2015-10-07T13:23:01.000</Timestamp>\n" + 
		"    <EquipmentIdentifier nameSpaceID=\"\">" + MOCK_MDS_DEVICE_ID + "</EquipmentIdentifier>\n" + 
		"  </Observation>\n" + 
		"  <VMD id=\"MDC_DEV_ECG_RESP_VMD\" index=\"1\">\n" + 
		"    <Observation hierarchy=\"1.1.0.0\" index=\"0\" setid=\"\" uid=\"39cde8ee-5419-4ac9-b5e9-527abe7bc81d\">\n" + 
		"      <ObsIdentifier codingSystemName=\"MDC\" id=\"70666\" notFound=\"true\">MDC_DEV_ECG_RESP_VMD</ObsIdentifier>\n" + 
		"      <Timestamp>2015-10-07T13:23:01.000</Timestamp>\n" + 
		"      <EquipmentIdentifier nameSpaceID=\"\">" + MOCK_VMD_DEVICE_ID + "</EquipmentIdentifier>\n" + 
		"    </Observation>\n" + 
		"    <CHAN id=\"MDC_DEV_ARRHY_CHAN\" index=\"1\">\n" + 
		"      <Observation hierarchy=\"1.1.1.0\" index=\"0\" setid=\"\" uid=\"670e93d3-03d8-43d8-98c6-372e9d284730\">\n" + 
		"        <ObsIdentifier codingSystemName=\"MDC\" id=\"70671\" notFound=\"true\">MDC_DEV_ARRHY_CHAN</ObsIdentifier>\n" + 
		"        <Timestamp>2015-10-07T13:23:01.000</Timestamp>\n" + 
		"      </Observation>\n" + 
		"      <Metric index=\"1\">\n" + 
		"        <Observation hierarchy=\"1.1.1.2\" index=\"2\" setid=\"\" uid=\"9bad433c-5417-4e57-b860-52fab5d21999\">\n" + 
		"    <ObsIdentifier codingSystemName=\"MDC\" id=\"147842\">MDC_ECG_CARD_BEAT_RATE</ObsIdentifier>\n" + 
		"    <ObsIdentifier codingSystemName=\"MDC\" isAlternate=\"true\">MDC_ECG_HEART_RATE</ObsIdentifier>\n" + 
		"    <Value typeHL7v2=\"NM\">80</Value>\n" + 
		"    <Unit codingSystemName=\"MDC\" id=\"264864\">MDC_DIM_BEAT_PER_MIN</Unit>\n" + 
		"    <Timestamp>2015-10-07T13:23:01.000</Timestamp>\n" + 
		"        </Observation>\n" + 
		"      </Metric>\n" + 
		"    </CHAN>\n" + 
		"  </VMD>\n" + 
		"      </MDS>\n" + 
		"    </Order_Observations>\n" + 
		"  </Patient_Result>\n" + 
		"</PCD_01_Message>";
	
	
	PCD_01MessageToSqlObservationsConverter converter;

	@Before
	public void init() throws Exception {
		converter = new PCD_01MessageToSqlObservationsConverter();

	}

	@Test
	public void convertion() throws Exception {

		@SuppressWarnings("unused")
		List<SqlObservation> observations = converter.convertToSqlObservations(ParserUtils.parseXmlString(MOCK_MESSAGE));
		assertEquals(4, observations.size());
		
		SqlObservation obs = observations.get(3);
		assertEquals("80", obs.getValue());
		assertEquals("NM", obs.getValueType());
		assertEquals("MDC_ECG_CARD_BEAT_RATE", obs.getCode());
		assertEquals("MDC", obs.getCodeSystem());
	}
}
