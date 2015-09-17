package net.sllmdilab.t5.converters;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sllmdilab.commons.exceptions.T5Exception;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class XMLToRDFConverter {

	private static Logger logger = LoggerFactory.getLogger(XMLToRDFConverter.class);

	private final String nstBox;
	private final String nsaBox;
	private final OntModel modelTbox;
	private final OntProperty ontPropOrderNum;

	/**
	 * @param baseNS
	 *            Base namespace uri that will be used to create uri's for individuals
	 */
	public XMLToRDFConverter(String baseNS) {
		nstBox = baseNS + "/model#";
		nsaBox = baseNS + "/data#";

		modelTbox = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		// Create index property
		ontPropOrderNum = modelTbox.createDatatypeProperty(nstBox + "xmlSeqOrderIndex");
	}

	/**
	 * Takes any XML document, triplifies ( converts to RDF) result is bunch of triples in N-triple format - can be
	 * saved to file for bulk load or send in HTTP POST payload.
	 */
	public String triplifyXMLDoc(Document doc) {

		OntModel modelAbox = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		triplifyElement(doc.getDocumentElement(), -1, null, modelAbox);

		return serializeModel(modelAbox, "N-TRIPLE");
	}

	private void triplifyElement(Element elemCurrent, int tagIndex, Individual indParent, OntModel modelAbox) {

		HashMap<String, Object> tboxMap = new HashMap<String, Object>();
		// process current elem
		String className = generateClassName(elemCurrent.getNodeName());
		String classURI = nstBox + className;
		OntClass ontClass = getClassDefinition(tboxMap, classURI);

		// Create instance
		String id = UUID.randomUUID().toString();
		Individual ind = modelAbox.createIndividual(nsaBox + className + "__" + id, ontClass);

		String value = getTrimmedValue(elemCurrent);
		if (!StringUtils.isBlank(value)) {
			String propName = Character.toLowerCase(className.charAt(0)) + className.substring(1) + "Value";
			OntProperty ontProp = getDatatypeOntProperty(tboxMap, nstBox + propName);

			ind.addLiteral(ontProp, value);
		}

		// add orderNum property if given
		if (tagIndex != -1) {
			ind.addLiteral(ontPropOrderNum, new Integer(tagIndex));
		}

		handleAttributes(tboxMap, ind, elemCurrent.getAttributes());

		// Process relation from parent to current element
		if (indParent != null) {
			// the relation name is inherited from class name of current element
			OntProperty ontProp = getObjectOntProperty(tboxMap, nstBox + "has" + className);

			// link parent resource and this resource with object property
			indParent.addProperty(ontProp, ind);
		}

		// Recursion over children of this element
		triplifyChildNodes(modelAbox, ind, elemCurrent.getChildNodes());
	}

	private String getTrimmedValue(Element elemCurrent) {
		// Process text value. It is defined as attribute classnameValue - with
		// lower case start
		String value = null;
		Node node1 = elemCurrent.getChildNodes().item(0);
		if (node1 != null && node1.getNodeType() == Node.TEXT_NODE) {
			value = node1.getTextContent();
			if (value != null) {
				value = value.trim();
			}
		}
		return value;
	}

	private OntClass getClassDefinition(HashMap<String, Object> tboxMap, String classURI) {
		OntClass ontClass = null;
		if (!tboxMap.containsKey(classURI)) {
			// Create the class definition
			ontClass = modelTbox.createClass(classURI);
			tboxMap.put(classURI, ontClass);
			logger.debug(classURI);
		} else {
			ontClass = (OntClass) tboxMap.get(classURI);
		}
		return ontClass;
	}

	private String generateClassName(String nodeName) {
		return Character.toUpperCase(nodeName.charAt(0)) + nodeName.substring(1);
	}

	private void handleAttributes(HashMap<String, Object> tboxMap, Individual ind, NamedNodeMap mapAttr) {
		for (int i = 0; i < mapAttr.getLength(); i++) {
			Node nodeattr = mapAttr.item(i);
			String attrname = nodeattr.getNodeName();
			String attrns = nodeattr.getNamespaceURI();
			String attrvalue = nodeattr.getNodeValue();

			if (StringUtils.isBlank(attrvalue)) {
				continue;
			}

			if (attrns == null) {
				attrns = nstBox;
			} else if (!(attrns.endsWith("/") || attrns.endsWith("#"))) {
				attrns += "#";
			}

			OntProperty ontProp = getDatatypeOntProperty(tboxMap, attrns + attrname);

			// Add literal
			ind.addLiteral(ontProp, attrvalue.trim());
		}
	}

	private void triplifyChildNodes(OntModel modelAbox, Individual ind, NodeList nlist) {
		// Elements order may be relevant. Keep track of index within same tag name group

		String currentTagName = "";
		int elemTagIndex = 0;

		for (int j = 0; nlist != null && j < nlist.getLength(); j++) {
			Node node = nlist.item(j);
			if (node.getNodeType() != Node.ELEMENT_NODE)
				continue;
			Element elem = (Element) nlist.item(j);
			if (elem.getNodeName().equals(currentTagName)) {
				elemTagIndex++;
			} else {
				elemTagIndex = 0;
				currentTagName = elem.getNodeName();
			}

			triplifyElement(elem, elemTagIndex, ind, modelAbox);
		}
	}

	private OntProperty getDatatypeOntProperty(HashMap<String, Object> tboxMap, String propURI) {
		OntProperty ontProp;
		if (!tboxMap.containsKey(propURI)) {
			// Create the property definition
			ontProp = modelTbox.createDatatypeProperty(propURI);
			tboxMap.put(propURI, ontProp);
			logger.debug(propURI);
		} else {
			ontProp = (OntProperty) tboxMap.get(propURI);
		}
		return ontProp;
	}

	private OntProperty getObjectOntProperty(HashMap<String, Object> tboxMap, String propURI) {
		OntProperty ontProp;
		if (!tboxMap.containsKey(propURI)) {
			// Create the property definition
			ontProp = modelTbox.createObjectProperty(propURI);
			tboxMap.put(propURI, ontProp);
			logger.debug(propURI);
		} else {
			ontProp = (OntProperty) tboxMap.get(propURI);
		}
		return ontProp;
	}

	private String serializeModel(Model model, String format) {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		model.write(bos, format);

		try {
			return new String(bos.toByteArray(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new T5Exception(e);
		}
	}

	private void writeToFile(File file, String content) throws IOException {
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
			bw.write(content);
		}
	}

	private void run(String[] args) throws ParserConfigurationException, UnsupportedEncodingException {
		String sourcefile = args[0];
		String targetAboxFile = args[1];
		String targetTboxFile = args[2];

		Document doc = null;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder;

		builder = factory.newDocumentBuilder();

		try {
			doc = builder.parse(sourcefile);
		} catch (SAXException | IOException e) {
			System.err.println("Error parsing source file " + sourcefile);
			e.printStackTrace();
			System.exit(-1);
		}

		long start = System.currentTimeMillis();

		String triples = triplifyXMLDoc(doc);

		String model = serializeModel(modelTbox, "TURTLE");

		try {
			writeToFile(new File(targetTboxFile), model);
		} catch (IOException e) {
			System.err.println("Error writing to file " + targetTboxFile);
			e.printStackTrace();
			System.exit(-1);
		}
		try {
			writeToFile(new File(targetAboxFile), triples);
		} catch (IOException e) {
			System.err.println("Error writing to file " + targetAboxFile);
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("time: " + (System.currentTimeMillis() - start) + " msec");

		System.out.println("DONE");
	}

	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
		(new XMLToRDFConverter("http://sll-mdilab.net/T5")).run(args);
	}
}
