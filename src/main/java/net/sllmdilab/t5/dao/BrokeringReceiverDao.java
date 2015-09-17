package net.sllmdilab.t5.dao;

import java.io.IOException;
import java.util.List;

import net.sllmdilab.commons.database.MLDBClient;
import net.sllmdilab.commons.exceptions.T5Exception;
import net.sllmdilab.t5.domain.BrokeringReceiver;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@Repository
public class BrokeringReceiverDao {
	
	@Autowired
	private MLDBClient mldbClient;
	
	static class BrokeringReceiverList {
		@JacksonXmlElementWrapper(useWrapping = false, localName="receivers")
		public List<BrokeringReceiver> receiver;
	}
	
	private String createFindAllForDeviceIdXQuery(String deviceId) {
		//@formatter:off
		String xQuery = 
			"xquery version '1.0-ml';\n"+
			"declare default element namespace 'http://hl7.org/fhir';\n"+
			"<brokeringReceiverList>\n"+
			"{\n"+
			"let $extensionUrl := 'http://sll-mdilab.net/fhir/DeviceUseStatement#brokeringReceiver'\n"+
			"let $deviceId := 'Device/" +  StringEscapeUtils.escapeXml10(deviceId) + "'\n"+
			"let $recIds := /DeviceUseStatement[not(whenUsed/end) and device/reference/@value = $deviceId]/extension[@url = $extensionUrl]/valueResource/reference/@value\n"+
			"for $recId in $recIds\n"+
			"let $brs := /BrokeringReceiver[id/@value = fn:replace($recId,\'^.*/\', \'\') ]\n"+
			"for $br in $brs\n"+
			"return \n"+
			"<receiver><address>{data($br/address/@value)}</address><port>{data($br/port/@value)}</port></receiver>\n"+
			"}\n"+
			"</brokeringReceiverList>";
		//@formatter:on
		return xQuery;
	}

	
	public List<BrokeringReceiver> findAllActiveForDeviceId(String deviceId) {
		String result = mldbClient.sendQuery(createFindAllForDeviceIdXQuery(deviceId));
		ObjectMapper xmlMapper = new XmlMapper();
		
		try {
			BrokeringReceiverList receivers = xmlMapper.readValue(result, BrokeringReceiverList.class);
			return receivers.receiver;
		} catch (IOException e) {
			throw new T5Exception("", e);
		}
	}

}
