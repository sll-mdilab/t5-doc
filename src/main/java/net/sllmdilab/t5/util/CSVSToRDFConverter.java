package net.sllmdilab.t5.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.UUID;

import au.com.bytecode.opencsv.CSVReader;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class CSVSToRDFConverter {

	private String sourceFilesDir;
	private String className;

	private OntModel tboxModel;
	private OntModel aboxModel;
	private String separator;
	private String textFieldSeparator = null;
	private String pathSeparator;
	private int keyColIndex = -1;

	private String nsTBox;
	private String nsABox;

	private String getUniqueIdentifier() {
		return UUID.randomUUID().toString();
	}

	public CSVSToRDFConverter(String sourceFilesDir, String className, String baseNameSpace, String separator,
			String textFieldSeparator, int keyColIndex) {

		nsTBox = baseNameSpace + "model#";
		nsABox = baseNameSpace + "data#";

		this.sourceFilesDir = sourceFilesDir;
		this.className = className;
		this.separator = separator;
		this.textFieldSeparator = textFieldSeparator;

		tboxModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		aboxModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);

		pathSeparator = System.getProperty("file.separator");
		this.keyColIndex = keyColIndex;
	}

	public void extract(String targetPath) throws IOException {

		String pathTBox = targetPath + pathSeparator + "TBox_" + className + ".nt";
		File dir = new File(sourceFilesDir);
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isHidden()) {
				continue;
			}
			readFile(files[i].getAbsolutePath(), pathTBox, targetPath + pathSeparator + "ABox_" + className + "_" + i
					+ ".nt");
		}
		saveModel(tboxModel, pathTBox);
	}

	public void readFile(String sourceFilePath, String pathTBox, String pathABox) throws IOException {
		System.out.println(sourceFilePath);

		aboxModel.removeAll();

		OntClass ontClass;
		Individual ind = null;
		OntProperty[] ontProps;
		OntProperty propRowNum;
		OntProperty propSourceFile;
		int numProps = 0;

		// name of File becomes owl:Class
		File file = new File(sourceFilePath);
		String sourceFileName = file.getName();
		String clsName = className;
		String clsURI = nsTBox + clsName;
		System.out.println("Create owl class: " + clsURI);
		ontClass = tboxModel.createClass(clsURI);

		List<String[]> lines = null;
		// Open CSV file
		FileInputStream is = new FileInputStream(new File(sourceFilePath));
		char c = separator.charAt(0);
		CSVReader reader = new CSVReader(new InputStreamReader(is, "UTF-8"), c);
		lines = reader.readAll();
		reader.close();

		propRowNum = tboxModel.createDatatypeProperty(nsTBox + "rowNum");
		propSourceFile = tboxModel.createDatatypeProperty(nsTBox + "sourceFileName");

		String[] propNames = lines.get(0);
		ontProps = new OntProperty[propNames.length];

		for (int i = 0; i < propNames.length; i++) {
			if (propNames[i].length() == 0) {
				break;
			}
			String propName = propNames[i].replace("\"", "");
			propName = propName.replace(" ", "_").replace("/", "_").replace("\\", "_").replace(":", "_")
					.replace(".", "_").replace(";", "_");
			propName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
			propName = nsTBox + propName;
			ontProps[i] = tboxModel.createDatatypeProperty(propName);
			numProps++;
		}

		for (int j = 1; j < lines.size() /* && j<3 */; j++) {
			System.out.println(j + " out of " + (lines.size() - 1));

			String[] propValues = lines.get(j);

			// Create an individual
			// If key column is given use its value in URI
			if (keyColIndex != -1) {
				String key = propValues[keyColIndex];
				key = safeChar(key);
				ind = aboxModel.createIndividual(nsABox + clsName + "__" + key, ontClass);
			} else {
				ind = aboxModel.createIndividual(nsABox + clsName + "__" + getUniqueIdentifier(), ontClass);
			}

			// add source file name
			ind.addLiteral(propSourceFile, sourceFileName);
			// add row index
			ind.addLiteral(propRowNum, new Integer(j));

			for (int k = 0; k < propValues.length && k < numProps; k++) {
				String propValue = propValues[k];
				propValue = propValue.replaceAll("\"", "");
				propValue = propValue.trim();

				if (propValue.equals("")) {
					continue;
				}

				// If Literal
				try {
					Double fval = new Double(propValue);
					// If integer or double
					if (propValue.indexOf(".") == -1) {
						// Integer
						Long ival = new Long((long) fval.doubleValue());
						ind.addLiteral(ontProps[k], ival);
					} else {
						ind.addLiteral(ontProps[k], fval);
					}
				} catch (Exception e) {
					// No it is not number - create string literal
					try {
						if (textFieldSeparator == null) {
							ind.addLiteral(ontProps[k], propValue);
						} else {
							String[] vals = propValue.split(textFieldSeparator);
							for (String val : vals) {
								ind.addLiteral(ontProps[k], val);
							}
						}
					} catch (Exception e2) {
						System.err.println(ontProps[k] + ":" + propValue);
					}
				}
			}

		}
		saveModel(tboxModel, pathTBox);
		saveModel(aboxModel, pathABox);
	}

	public String safeChar(String input) {
		String str = input.replace(".", "_");
		str = input.replace(" ", "_");

		// should not start with a number
		if (Character.isDigit(str.charAt(0))) {
			str = "_" + str;
		}

		char[] allowed = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_".toCharArray();
		char[] charArray = str.toString().toCharArray();
		StringBuilder result = new StringBuilder();
		for (char c : charArray) {
			for (char a : allowed) {
				if (c == a) {
					result.append(a);
					break;
				}
			}
		}

		return result.toString();
	}

	public void saveModel(OntModel model, String path) throws IOException {

		File file = new File(path);
		file.createNewFile();
		FileOutputStream fos = new FileOutputStream(file, false);
		OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8");
		model.write(writer, "N-TRIPLES");
	}

	public static void main(String[] args) throws IOException {

		String sourceFilesDir = args[0];
		String className = args[1];
		String targetDir = args[2];
		String baseNameSpace = args[3];
		String separator = args[4];

		// Separator within a text field, to use splitting one cell into separate sub strings. Sometimes an array of
		// values are concatenated in one cell
		String textFieldSeparator = null;
		if (args.length > 5 && !args[5].equals("null")) {
			textFieldSeparator = args[5];
		}

		int keyColIndex = -1;
		if (args.length > 6 && !args[6].equals("null")) {
			keyColIndex = new Integer(args[6]).intValue();
		}

		CSVSToRDFConverter reader = new CSVSToRDFConverter(sourceFilesDir, className, baseNameSpace, separator,
				textFieldSeparator, keyColIndex);
		reader.extract(targetDir);

		System.out.println("DONE");
	}

}
