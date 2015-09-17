package net.sllmdilab.t5.dao;

import net.sllmdilab.commons.database.MLDBClient;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PatientIdentificationDao {

	@Autowired
	private MLDBClient mldbClient;
	
	private String createFindAssociatedPatientIdXQuery(String deviceId) {
		//@formatter:off
		
		String xQuery = 
			"xquery version '1.0-ml';\n"+
			"declare default element namespace 'http://hl7.org/fhir';\n"+
			"let $deviceId := 'Device/" +StringEscapeUtils.escapeXml10(deviceId) +"'\n"+
			"return \n"+
			"/DeviceUseStatement[device/reference/@value = $deviceId and not (whenUsed/end)]/subject/reference/@value ";
		//@formatter:on
		return xQuery;
	}
	
	public String findAssociatedPatientId(String deviceId) {
		return mldbClient.sendQuery(createFindAssociatedPatientIdXQuery(deviceId));
	}

}
