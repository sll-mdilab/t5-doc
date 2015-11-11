package net.sllmdilab.t5.converters;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.primitive.CommonTM;
import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.CX;
import ca.uhn.hl7v2.model.v26.datatype.DTM;
import ca.uhn.hl7v2.model.v26.datatype.EI;
import ca.uhn.hl7v2.model.v26.datatype.IS;
import ca.uhn.hl7v2.model.v26.datatype.PL;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v26.group.ORU_R01_VISIT;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import ca.uhn.hl7v2.model.v26.segment.OBX;
import ca.uhn.hl7v2.model.v26.segment.PID;
import net.sllmdilab.commons.exceptions.RosettaLookupException;
import net.sllmdilab.commons.t5.validators.RosettaValidator;
import net.sllmdilab.commons.util.Constants;
import net.sllmdilab.t5.exceptions.T5ConversionException;

public class PCD_01MessageToXMLConverter {

	private static Logger logger = LoggerFactory.getLogger(PCD_01MessageToXMLConverter.class);

	private static final String WAVEFORM_MATCH = "WAVEFORM";

	@Autowired
	private RosettaValidator rosettaValidator;

	public Document getXML(Message msg) {

		try {
			long start_ts = System.currentTimeMillis();

			logger.info(msg.getClass().getSimpleName());
			ORU_R01 msgORU = (ORU_R01) msg;

			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			MSH msh = msgORU.getMSH();

			Element root = createRootElement(doc, msh);
			doc.appendChild(root);

			root.appendChild(createSendingApplicationElement(doc, msh));
			root.appendChild(createSendingFacilityElement(doc, msh));

			handlePatientResults(msgORU, doc, root);

			long end_ts = System.currentTimeMillis();
			logger.info("XML conversion took " + (end_ts - start_ts) + " millisec");

			return doc;

		} catch (HL7Exception | DOMException | ParserConfigurationException e) {
			throw new T5ConversionException(e);
		}
	}

	private void handlePatientResults(ORU_R01 msgORU, Document doc, Element root) {
		// PATIENT_RESULTS - we must be prepared for multiple patient stuff sent
		// in one message

		int numOfPatients = msgORU.getPATIENT_RESULTReps();
		logger.debug("Number of PATIENT_RESULT = " + numOfPatients);
		for (int i = 0; i < numOfPatients; i++) {
			handlePatientResult(msgORU.getPATIENT_RESULT(i), root, doc);
		}
	}

	private Element createRootElement(Document doc, MSH msh) throws DataTypeException {
		Element root = doc.createElement("PCD_01_Message");
		root.setAttribute("id", msh.getMsh10_MessageControlID().getValueOrEmpty());
		root.setAttribute("timeStamp", convertDtmToXmlDate(msh.getMsh7_DateTimeOfMessage()));
		return root;
	}

	private Element createSendingFacilityElement(Document doc, MSH msh) throws HL7Exception {
		Element elemFacility = doc.createElement("Sending_Facility");

		elemFacility.setAttribute("idLocal", msh.getMsh4_SendingFacility().getHd1_NamespaceID().getValueOrEmpty());
		if (msh.getMsh4_SendingFacility().getHd2_UniversalID().isEmpty()) {
			elemFacility.setAttribute("idUniversal", msh.getMsh4_SendingFacility().getHd2_UniversalID()
					.getValueOrEmpty());
		}
		if (msh.getMsh4_SendingFacility().getHd3_UniversalIDType().isEmpty()) {
			elemFacility.setAttribute("idUniversalType", msh.getMsh4_SendingFacility().getHd3_UniversalIDType()
					.getValueOrEmpty());
		}
		return elemFacility;
	}

	private Element createSendingApplicationElement(Document doc, MSH msh) throws HL7Exception {
		Element elemApp = doc.createElement("Sending_Application");

		String messageSenderLocalID = msh.getMsh3_SendingApplication().getHd1_NamespaceID().getValueOrEmpty();
		elemApp.setAttribute("idLocal", messageSenderLocalID);
		if (msh.getMsh3_SendingApplication().getHd2_UniversalID().isEmpty()) {
			String messageSenderGlobalID = msh.getMsh3_SendingApplication().getHd2_UniversalID().getValueOrEmpty();
			elemApp.setAttribute("idUniversal", messageSenderGlobalID);
		}
		if (msh.getMsh3_SendingApplication().getHd3_UniversalIDType().isEmpty())
			elemApp.setAttribute("idUniversalType", msh.getMsh3_SendingApplication().getHd3_UniversalIDType()
					.getValueOrEmpty());
		return elemApp;
	}

	private void handlePatientResult(ORU_R01_PATIENT_RESULT pr, Element elemRoot, Document doc) {
		Element elemPR = doc.createElement("Patient_Result");
		elemRoot.appendChild(elemPR);

		handlePatientData(pr.getPATIENT(), elemPR, doc);

		int numObsOrders = pr.getORDER_OBSERVATIONReps();
		for (int i = 0; i < numObsOrders; i++) {
			handleOrderObservations(pr.getORDER_OBSERVATION(i), elemPR, doc);
		}
	}

	private void handlePatientData(ORU_R01_PATIENT patient, Element elemPR, Document doc) {
		Element elemPatient = doc.createElement("Patient");
		elemPR.appendChild(elemPatient);

		PID pid = patient.getPID();
		int numOfPatientIDs = pid.getPatientIdentifierListReps();
		for (int i = 0; i < numOfPatientIDs; i++) {
			Element elemID = doc.createElement("Identifier");
			elemPatient.appendChild(elemID);
			CX plist = pid.getPatientIdentifierList(i);
			elemID.setTextContent(plist.getIDNumber().getValueOrEmpty());
			elemID.setAttribute("authority", plist.getAssigningAuthority().getNamespaceID().getValueOrEmpty());
			elemID.setAttribute("typeCode", plist.getIdentifierTypeCode().getValueOrEmpty());
		}
		
		// handle visit
		ORU_R01_VISIT visit = patient.getVISIT();
		PL location = visit.getPV1().getAssignedPatientLocation();
		IS pointOfCare = location.getPointOfCare();
		IS room = location.getRoom();
		IS bed = location.getBed();
		String pointOfCareID = pointOfCare.getValueOrEmpty();
		String bedID = bed.getValueOrEmpty();
		String roomID = room.getValueOrEmpty();	
		// if there is either bed or room given, create an object
		if( !pointOfCareID.equals("") || !bedID.equals("") || !roomID.equals("")){
			Element elemLocation = doc.createElement("Location");
			elemPatient.appendChild(elemLocation);
			elemLocation.setAttribute("pointOfCare", pointOfCareID);
			elemLocation.setAttribute("bed", bedID);
		}
		
	}

	private void handleOrderObservations(ORU_R01_ORDER_OBSERVATION orderObservation, Element elemPR, Document doc)
			throws T5ConversionException {
		try {
			Map<String, Element> mapMDS = new HashMap<String, Element>();
			Map<String, Element> mapVMD = new HashMap<String, Element>();
			Map<String, Element> mapCHAN = new HashMap<String, Element>();
			Map<String, Element> mapMetric = new HashMap<String, Element>();

			Element elemObsOrders = doc.createElement("Order_Observations");
			elemPR.appendChild(elemObsOrders);

			DTM orderDateTime = orderObservation.getOBR().getObr7_ObservationDateTime();
			Element elemOrder = doc.createElement("Order");
			elemOrder.setAttribute("timeStamp", convertDtmToXmlDate(orderDateTime));

			if (!orderObservation.getOBR().getObr8_ObservationEndDateTime().isEmpty()) {
				orderDateTime = orderObservation.getOBR().getObr8_ObservationEndDateTime();
				elemOrder.setAttribute("timeStampEnd", convertDtmToXmlDate(orderDateTime));
			}
			elemObsOrders.appendChild(elemOrder);

			// Filler Order Number - OBR-3
			elemOrder.appendChild(createFillerOrderNumberElement(orderObservation, doc));

			// Universal Service ID OBR-4
			Element elemService = doc.createElement("UniversalServiceID");

			String serviceID_id = "";
			CWE serviceID = orderObservation.getOBR().getObr4_UniversalServiceIdentifier();
			if (!serviceID.getCwe2_Text().isEmpty()) {
				elemService.setTextContent(serviceID.getCwe2_Text().getValue());
			}

			if (!serviceID.getCwe1_Identifier().isEmpty()) {
				serviceID_id = serviceID.getCwe1_Identifier().getValue();
				elemService.setAttribute("id", serviceID_id);
			}

			if (!serviceID.getCwe3_NameOfCodingSystem().isEmpty()) {
				elemService.setAttribute("codingSystemName", serviceID.getCwe3_NameOfCodingSystem().getValue());
			}

			elemOrder.appendChild(elemService);

			for (ORU_R01_OBSERVATION observation : orderObservation.getOBSERVATIONAll()) {
				handleObservation(observation, doc, elemObsOrders, elemOrder, orderDateTime, serviceID_id, mapMDS,
						mapVMD, mapCHAN, mapMetric);
			}

		} catch (HL7Exception | DOMException | XPathExpressionException e) {
			throw new T5ConversionException(e);
		}
	}

	private void handleObservation(ORU_R01_OBSERVATION observation, Document doc, Element elemObsOrders,
			Element elemOrder, DTM orderDateTime, String serviceID_id, Map<String, Element> mapMDS,
			Map<String, Element> mapVMD, Map<String, Element> mapCHAN, Map<String, Element> mapMetric)
			throws HL7Exception, DataTypeException, XPathExpressionException {
		OBX obx = observation.getOBX();

		// Attributes of Observation itself - from OBX segment
		Element elemObs = doc.createElement("Observation");
		elemObsOrders.appendChild(elemObs);

		// Observation UID based on generated GUID
		String guidObs = UUID.randomUUID().toString();
		elemObs.setAttribute("uid", guidObs);

		Element elemObsIdentifier = createObsIdentifierElement(doc, obx);
		elemObs.appendChild(elemObsIdentifier);

		handleAlternativeIdentifiers(doc, obx, elemObs, elemObsIdentifier);

		handleHierarchy(doc, elemObsOrders, elemOrder, serviceID_id, obx, elemObs, obx.getObservationSubID()
				.getValueOrEmpty(), mapMDS, mapVMD, mapCHAN, mapMetric);

		// OBX-2 setid
		elemObs.setAttribute("setid", obx.getObx1_SetIDOBX().getValue());

		// May be multiple values
		for (Varies value : obx.getObservationValue()) {
			elemObs.appendChild(createValueElement(doc, obx, value));
		}

		// Unit
		CWE units = obx.getObx6_Units();
		if (!units.getCwe1_Identifier().isEmpty()) {
			elemObs.appendChild(createUnitElement(doc, units));
		}

		elemObs.appendChild(createTimestampElement(doc, orderDateTime, obx, elemObs));

		createEquipmentIdentifiers(doc, obx, elemObs);
	}

	private void handleHierarchy(Document doc, Element elemObsOrders, Element elemOrder, String serviceID_id, OBX obx,
			Element elemObs, String hierarchyString, Map<String, Element> mapMDS, Map<String, Element> mapVMD,
			Map<String, Element> mapCHAN, Map<String, Element> mapMetric) {
		elemObs.setAttribute("hierarchy", obx.getObservationSubID().getValueOrEmpty());

		// Need to analyze OBX-4 in terms of where in Containment Tree this OBX belongs
		String[] hierarchyArray = hierarchyString.split("\\.");
		if (hierarchyArray.length < 4) {
			// OBX-4 must be given and contain at least 4 levels
			elemObs.setAttribute("violatesIHE", "true");
			elemOrder.appendChild(elemObs);
		} else {
			// Set index attribute of this metric in containment tree
			elemObs.setAttribute("index", hierarchyArray[hierarchyArray.length - 1]);

			// Num of MDS segments, as these may be nested
			int numMDS = getNumMds(serviceID_id, hierarchyArray);

			// These hashmaps keep track of containment tree indexes and XML elements at each level
			Element elemMDS = createMdsElement(doc, elemObsOrders, hierarchyArray, numMDS, mapMDS);

			int level = numMDS;

			// case where OBX belongs to MDS
			if (!hierarchyArray[level - 1].equals("0") && hierarchyArray[level].equals("0")
					&& hierarchyArray[level + 1].equals("0") && hierarchyArray[level + 2].equals("0")) {
				elemMDS.appendChild(elemObs);
				// if x.0.0.0 then this OBX gives MDS identifier, set it explicitly
				if (obx.getObx3_ObservationIdentifier().getCwe2_Text().getValueOrEmpty().endsWith("_MDS")) {
					elemMDS.setAttribute("id", obx.getObx3_ObservationIdentifier().getCwe2_Text().getValueOrEmpty());
				}
			} else {
				handleVmd(doc, obx, elemObs, hierarchyArray, elemMDS, mapVMD, mapCHAN, mapMetric, level);
			}
		}
	}

	private int getNumMds(String serviceID_id, String[] hierarchyArray) {
		if (serviceID_id.contains(WAVEFORM_MATCH)) {
			return 1;
		} else {
			return hierarchyArray.length - 3;
		}
	}

	private void handleVmd(Document doc, OBX obx, Element elemObs, String[] hierarchyArray, Element elemMDS,
			Map<String, Element> mapVMD, Map<String, Element> mapCHAN, Map<String, Element> mapMetric, int level) {
		// Get VMD element for this OBX
		Element elemVMD = getVmdElement(doc, hierarchyArray, elemMDS, mapVMD, level);

		level++;
		if (!hierarchyArray[level - 1].equals("0") && hierarchyArray[level].equals("0")
				&& hierarchyArray[level + 1].equals("0")) {
			elemVMD.appendChild(elemObs);
			// if x.y.0.0 then this OBX gives VMD identifier, set it explicitly
			if (obx.getObx3_ObservationIdentifier().getCwe2_Text().getValueOrEmpty().endsWith("_VMD")) {
				elemVMD.setAttribute("id", obx.getObx3_ObservationIdentifier().getCwe2_Text().getValueOrEmpty());
			}
		} else {
			handleChan(doc, obx, elemObs, hierarchyArray, mapCHAN, mapMetric, level, elemVMD);
		}
	}

	private Element getVmdElement(Document doc, String[] hierarchyArray, Element elemMDS, Map<String, Element> mapVMD,
			int level) {
		Element elemVMD;
		String key = calcKey(hierarchyArray, level);
		if (mapVMD.containsKey(key)) {
			elemVMD = mapVMD.get(key);
		} else {
			elemVMD = doc.createElement("VMD");
			elemVMD.setAttribute("index", hierarchyArray[level]);
			mapVMD.put(key, elemVMD);
			elemMDS.appendChild(elemVMD);
		}
		return elemVMD;
	}

	private void handleChan(Document doc, OBX obx, Element elemObs, String[] hierarchyArray,
			Map<String, Element> mapCHAN, Map<String, Element> mapMetric, int level, Element elemVMD) {

		// Get CHAN element for this OBX
		Element elemCHAN = getChanElement(doc, hierarchyArray, mapCHAN, level, elemVMD);

		level++;
		// This OBX is under CHAN
		if (!hierarchyArray[level - 1].equals("0") && hierarchyArray[level].equals("0")) {
			elemCHAN.appendChild(elemObs);
			// if x.y.z.0 then this OBX gives CHAN identifier, set it explicitly
			if (obx.getObx3_ObservationIdentifier().getCwe2_Text().getValueOrEmpty().endsWith("_CHAN")) {
				elemCHAN.setAttribute("id", obx.getObx3_ObservationIdentifier().getCwe2_Text().getValueOrEmpty());
			}
		} else {
			// Get Metric element for this OBX
			Element elemMetric = getMetricElement(doc, hierarchyArray, mapMetric, level, elemCHAN);

			// Check if there is a Facet
			String key = calcKey(hierarchyArray, level + 1);
			if (key == null) {
				elemMetric.appendChild(elemObs);
			} else {
				// there is a facet
				level++;
				elemMetric.appendChild(createFacetElement(doc, elemObs, hierarchyArray, level));
			}
		}
	}

	private Element getMetricElement(Document doc, String[] hierarchyArray, Map<String, Element> mapMetric, int level,
			Element elemCHAN) {
		String key = calcKey(hierarchyArray, level);

		Element elemMetric = null;
		if (mapMetric.containsKey(key)) {
			elemMetric = mapMetric.get(key);
		} else {
			elemMetric = doc.createElement("Metric");
			elemMetric.setAttribute("index", hierarchyArray[level]);
			elemCHAN.appendChild(elemMetric);
			mapMetric.put(key, elemMetric);
		}
		return elemMetric;
	}

	private Element getChanElement(Document doc, String[] hierarchyArray, Map<String, Element> mapCHAN, int level,
			Element elemVMD) {
		String key;
		Element elemCHAN = null;

		key = calcKey(hierarchyArray, level);
		if (mapCHAN.containsKey(key)) {
			elemCHAN = mapCHAN.get(key);
		} else {
			elemCHAN = doc.createElement("CHAN");
			elemCHAN.setAttribute("index", hierarchyArray[level]);
			elemVMD.appendChild(elemCHAN);
			mapCHAN.put(key, elemCHAN);
		}
		return elemCHAN;
	}

	private Element createFacetElement(Document doc, Element elemObs, String[] hierarchyArray, int level) {
		Element elemFacet = doc.createElement("Facet");
		elemFacet.setAttribute("index", hierarchyArray[level]);
		elemFacet.appendChild(elemObs);
		return elemFacet;
	}

	private Element createValueElement(Document doc, OBX obx, Varies value) {
		Element elemValue = doc.createElement("Value");
		elemValue.setTextContent(value.getData().toString());
		elemValue.setAttribute("typeHL7v2", obx.getObx2_ValueType().getValueOrEmpty());
		return elemValue;
	}

	private Element createUnitElement(Document doc, CWE units) {
		Element elemUnit = doc.createElement("Unit");
		elemUnit.setTextContent(units.getCwe2_Text().getValueOrEmpty());
		elemUnit.setAttribute("id", units.getCwe1_Identifier().getValueOrEmpty());
		elemUnit.setAttribute("codingSystemName", units.getCwe3_NameOfCodingSystem().getValueOrEmpty());
		return elemUnit;
	}

	private Element createMdsElement(Document doc, Element elemObsOrders, String[] hierarchyArray, int numMDS,
			Map<String, Element> mapMDS) {
		// Get MDS elements for this OBX they can be nested so iterate all
		Element elemMDS = null; // Last MDS elem

		for (int j = 0; j < numMDS; j++) {
			Element elemMDSCur = null;
			String key = calcKey(hierarchyArray, j);
			if (mapMDS.containsKey(key)) {
				elemMDSCur = mapMDS.get(key);
				elemMDS = elemMDSCur;
			} else {
				elemMDSCur = doc.createElement("MDS");
				mapMDS.put(key, elemMDSCur);
				elemMDSCur.setAttribute("index", hierarchyArray[j]);

				if (elemMDS == null) {
					elemObsOrders.appendChild(elemMDSCur);
				} else {
					elemMDS.appendChild(elemMDSCur);
				}
				elemMDS = elemMDSCur;
			}
		}
		return elemMDS;
	}

	private Element createTimestampElement(Document doc, DTM orderDateTime, OBX obx, Element elemObs)
			throws DataTypeException, XPathExpressionException {
		// Timestamp
		Element elemTS = doc.createElement("Timestamp");

		Date dateObs = obx.getObx14_DateTimeOfTheObservation().getValueAsDate();
		// If this OBX is not having its own timestamp, derive from
		// ORDER
		if (dateObs == null) {
			// Should search for TS in parents: CHAN, VMD, MDS
			String tsParent = null;

			// Find parent timestamp
			String xpathExpr = "../../Observation/Timestamp";
			XPath mXpath = XPathFactory.newInstance().newXPath();
			Object timenode = mXpath.evaluate(xpathExpr, elemObs, XPathConstants.NODE);
			if (timenode != null && timenode instanceof Node) {
				tsParent = ((Element) timenode).getTextContent();
			}

			if (tsParent == null) {
				elemTS.setTextContent(convertDtmToXmlDate(orderDateTime));
			} else {
				elemTS.setTextContent(tsParent);
			}
		} else {
			elemTS.setTextContent(convertDtmToXmlDate(obx.getObx14_DateTimeOfTheObservation()));
		}
		return elemTS;
	}

	private void handleAlternativeIdentifiers(Document doc, OBX obx, Element elemObs, Element elemObsIdentifier)
			throws HL7Exception {
		// Check RosettaTable
		boolean isInTermsTable = false;
		if (obx.getObx3_ObservationIdentifier().getCwe3_NameOfCodingSystem().getValueOrEmpty().equals("MDC")) {
			isInTermsTable = rosettaValidator.isInTermsTable(obx.getObx3_ObservationIdentifier().getCwe2_Text()
					.getValueOrEmpty());

			// Check for Synonyms - and add another identifier
			if (isInTermsTable) {
				String refid = obx.getObx3_ObservationIdentifier().getCwe2_Text().getValueOrEmpty();
				String refidSyn;
				try {
					refidSyn = rosettaValidator.getHarmonizedSynonym(refid);
					if (!StringUtils.isBlank(refidSyn)) {
						elemObsIdentifier = doc.createElement("ObsIdentifier");
						elemObs.appendChild(elemObsIdentifier);
						elemObsIdentifier.setTextContent(refidSyn);
						elemObsIdentifier.setAttribute("codingSystemName", "MDC");
						elemObsIdentifier.setAttribute("isAlternate", "true");
					}
				} catch (RosettaLookupException e) {
					// do nothing - did not get synonym
				}
			}
		}

		// If alternative Identifier exist, create another element
		if (!obx.getObx3_ObservationIdentifier().getCwe4_AlternateIdentifier().isEmpty()) {
			Element elemObsIdentifierAlt = doc.createElement("ObsIdentifier");
			elemObsIdentifierAlt.setAttribute("isAlternate", "true");
			elemObs.appendChild(elemObsIdentifierAlt);
			elemObsIdentifier.setAttribute("id", obx.getObx3_ObservationIdentifier().getCwe4_AlternateIdentifier()
					.getValueOrEmpty());
			if (obx.getObx3_ObservationIdentifier().getCwe5_AlternateText() != null) {
				elemObsIdentifierAlt.setTextContent(obx.getObx3_ObservationIdentifier().getCwe5_AlternateText()
						.getValueOrEmpty());
			}
			if (obx.getObx3_ObservationIdentifier().getCwe6_NameOfAlternateCodingSystem() != null) {
				elemObsIdentifierAlt.setAttribute("codingSystemName", obx.getObx3_ObservationIdentifier()
						.getCwe6_NameOfAlternateCodingSystem().getValueOrEmpty());
			}

			if (obx.getObx3_ObservationIdentifier().getCwe6_NameOfAlternateCodingSystem().getValueOrEmpty()
					.equals("MDC")) {
				isInTermsTable = rosettaValidator.isInTermsTable(obx.getObx3_ObservationIdentifier()
						.getCwe4_AlternateIdentifier().getValueOrEmpty());
			}
		}

		// If no MDC code was sent that is found also in Rosetta then add an attribute
		if (!isInTermsTable) {
			elemObsIdentifier.setAttribute("notFound", "true");
		}
	}

	private Element createObsIdentifierElement(Document doc, OBX obx) {
		// Extract identifier from OBX-3
		Element elemObsIdentifier = doc.createElement("ObsIdentifier");
		elemObsIdentifier.setTextContent(obx.getObx3_ObservationIdentifier().getCwe2_Text().getValueOrEmpty());
		elemObsIdentifier
				.setAttribute("id", obx.getObx3_ObservationIdentifier().getCwe1_Identifier().getValueOrEmpty());
		elemObsIdentifier.setAttribute("codingSystemName", obx.getObx3_ObservationIdentifier()
				.getCwe3_NameOfCodingSystem().getValueOrEmpty());
		return elemObsIdentifier;
	}

	private void createEquipmentIdentifiers(Document doc, OBX obx, Element elemObs) {
		// Equipment identifier
		EI[] ei = obx.getObx18_EquipmentInstanceIdentifier();
		for (int k = 0; k < ei.length; k++) {
			Element elemEI = doc.createElement("EquipmentIdentifier");
			elemObs.appendChild(elemEI);
			elemEI.setTextContent(ei[k].getEi1_EntityIdentifier().getValueOrEmpty());
			elemEI.setAttribute("nameSpaceID", ei[k].getEi2_NamespaceID().getValueOrEmpty());
		}
	}

	private Element createFillerOrderNumberElement(ORU_R01_ORDER_OBSERVATION order_OBSERVATION, Document doc)
			throws HL7Exception {
		Element elemFiller = doc.createElement("FillerOrderNumber");
		EI filler = order_OBSERVATION.getOBR().getFillerOrderNumber();

		if (!filler.getEi1_EntityIdentifier().isEmpty()) {
			elemFiller.setAttribute("idLocal", filler.getEi1_EntityIdentifier().getValue());
		}
		if (!filler.getEi2_NamespaceID().isEmpty()) {
			elemFiller.setAttribute("nameSpaceID", filler.getEi2_NamespaceID().getValue());
		}
		if (!filler.getEi3_UniversalID().isEmpty()) {
			elemFiller.setAttribute("idUniversal", filler.getEi3_UniversalID().getValue());
		}
		if (!filler.getEi4_UniversalIDType().isEmpty()) {
			elemFiller.setAttribute("idUniversalType", filler.getEi4_UniversalIDType().getValue());
		}
		return elemFiller;
	}

	/*
	 * This method is temporarily taken from the HAPI library and modified to fix a bug. See
	 * http://hl7api.sourceforge.net/
	 */
	private TimeZone getTimeZone(DTM dtm) throws DataTypeException {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, dtm.getHour());
		calendar.set(Calendar.MINUTE, dtm.getMinute());
		calendar.set(Calendar.SECOND, dtm.getSecond());

		float fractSecond = dtm.getFractSecond();
		calendar.set(Calendar.MILLISECOND, (int) Math.round(fractSecond * 1000.0));

		int gmtOff = dtm.getGMTOffset();
		if (gmtOff != CommonTM.GMT_OFFSET_NOT_SET_VALUE) {
			calendar.set(Calendar.ZONE_OFFSET, (gmtOff / 100) * (1000 * 60 * 60));

			/*
			 * The following sets the TimeZone associated with the returned calendar to use the offset specified in the
			 * value if this conflicts with the value it already contains.
			 * 
			 * This is needed in situations where daylight savings is in effect during part of the year, and a date is
			 * parsed which contains the other part of the year (i.e. parsing a DST DateTime when it is not actually DST
			 * according to the system clock).
			 * 
			 * See CommonTSTest#testGetCalendarRespectsDaylightSavings() for an example which fails if this is removed.
			 */
			if (calendar.getTimeZone().getRawOffset() != calendar.get(Calendar.ZONE_OFFSET)) {
				int hrOffset = gmtOff / 100;
				int minOffset = gmtOff % 100;
				StringBuilder tzBuilder = new StringBuilder("GMT");

				if (hrOffset < 0) {
					tzBuilder.append('-');
				} else {
					tzBuilder.append('+');
				}
				tzBuilder.append(Math.abs(hrOffset));
				tzBuilder.append(':');
				if (minOffset < 10) {
					tzBuilder.append('0');
				}
				tzBuilder.append(minOffset);

				calendar.setTimeZone(TimeZone.getTimeZone(tzBuilder.toString()));
			}
		}
		return calendar.getTimeZone();
	}

	private String convertDtmToXmlDate(DTM observationDateTime) throws DataTypeException {
		logger.info("Observation date: " + observationDateTime.getValue());
		
		Calendar dateOrderCalendar = observationDateTime.getValueAsCalendar();
		dateOrderCalendar.setTimeZone(getTimeZone(observationDateTime));

		return convertDateToXMLType(dateOrderCalendar.getTime());
	}

	private String calcKey(String[] hierarchyArray, int level) {
		// Avoid exception and handle the case if Facet is being tried on a tree which is up to Metric
		if (level >= hierarchyArray.length) {
			return null;
		}

		StringBuilder buf = new StringBuilder();
		for (int i = 0; i <= level; i++) {
			buf.append(hierarchyArray[i]);
			if (i < level) {
				buf.append(".");
			}
		}
		return buf.toString();
	}

	public static String convertDateToXMLType(Date date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.ISO_DATE_FORMAT);

		return ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC).format(formatter);
	}
}
