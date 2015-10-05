package net.sllmdilab.t5.dao;

import java.io.StringReader;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.sll_mdilab.t5.Observation;
import net.sll_mdilab.t5.Observations;
import net.sllmdilab.commons.database.MLDBClient;
import net.sllmdilab.commons.exceptions.T5Exception;
import net.sllmdilab.commons.util.T5FHIRUtils;

import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PCD01MessageDao {

	@Autowired
	private MLDBClient mldbClient;

	private String createQuery(String patientId, Date start, Date end, String observationTypeCode) {
		//@formatter:off
		String xQuery = "xquery version '1.0-ml';\n" + 
		"declare namespace fhir='http://hl7.org/fhir';\n" +
		"declare namespace t5='http://sll-mdilab.net/T5/';" + 
		"let $patientId := 'Patient/" + StringEscapeUtils.escapeXml10(patientId) + "'\n" + 
		"let $observationType := '" + StringEscapeUtils.escapeXml10(observationTypeCode) +"'\n" + 
		"let $timeFrom := '" + T5FHIRUtils.convertDateToXMLType(start) + "'\n" + 
		"let $timeUntil := '" + T5FHIRUtils.convertDateToXMLType(end) + "'\n" + 
		"let $statements := /fhir:DeviceUseStatement[fhir:subject/fhir:reference/@value = $patientId and fhir:whenUsed/fhir:start/@value <= $timeUntil and (fhir:whenUsed/fhir:end/@value >= $timeFrom or not(fhir:whenUsed/fhir:end))]\n" + 
		"return <t5:observations>{" +
		"for $statement in $statements\n" + 
		"return\n" + 
		"(\n" + 
			"let $deviceId := fn:replace(data($statement/fhir:device/fhir:reference/@value),'^.*/', '') \n" + 
			"let $statementTimeFrom := fn:max((xs:dateTime($statement/fhir:whenUsed/fhir:start/@value), xs:dateTime($timeFrom)))\n" + 
			"let $statementTimeUntil := fn:min((xs:dateTime($statement/fhir:whenUsed/fhir:end/@value), xs:dateTime($timeUntil)))\n" + 
			"return \n" + 
				"/PCD_01_Message/Patient_Result/Order_Observations/MDS[//Observation/EquipmentIdentifier = $deviceId]//Observation[ObsIdentifier = $observationType][Timestamp >= $statementTimeFrom and Timestamp <= $statementTimeUntil]\n" + 
		")}</t5:observations>";
		//@formatter:on

		return xQuery;
	}

	public List<Observation> findByPatientIdTimeAndCode(String patientId, Date start, Date end,
			String observationTypeCode) {
		String result = mldbClient.sendQuery(createQuery(patientId, start, end, observationTypeCode));

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Observations.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			
			StringReader resultReader = new StringReader(result);
			Observations observations  = (Observations) unmarshaller.unmarshal(resultReader);
			
			return observations.getObservations();
		} catch (JAXBException e) {
			throw new T5Exception("JAXB parse error.", e);
		}
	}
}
