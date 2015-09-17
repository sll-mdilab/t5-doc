package net.sllmdilab.t5.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

// This class splits a combined HL7 v2 message file, where messages are plainly concatenated.
// It extacts message timestamp and uses as a file name, in order to keep the time order so that files would be 
// alphabetically sorted based on timestamp
public class MessageFileSplitter {

	private String separator;

	public MessageFileSplitter() {
		separator = System.getProperty("file.separator");
	}

	public void execute(String sourceFilePath, String targetDir) throws UnsupportedEncodingException,
			FileNotFoundException, IOException {

		File dir = new File(targetDir);
		if (!dir.exists()) {
			dir.mkdir();
		}

		File fileSource = new File(sourceFilePath);
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileSource), "UTF-8"));) {
			String line;
			int docCount = 0;
			String currentTargetFilePath = null;
			ArrayList<String> currentDoc = new ArrayList<String>();
			while ((line = in.readLine()) != null) {
				if (line.trim().startsWith(".")) {
					continue;
				}

				if (line.trim().startsWith("MSH|")) {

					if (currentTargetFilePath != null) {
						writeToFileByLine(currentTargetFilePath, currentDoc);
					}

					String filename = line.substring(74, 86);
					currentDoc.clear();
					docCount++;
					currentTargetFilePath = targetDir + separator + "Msg_" + filename + "__" + docCount + "_.hl7";
				}
				currentDoc.add(line);
			}
		}
	}

	private void writeToFileByLine(String path, ArrayList<String> lines) throws UnsupportedEncodingException,
			FileNotFoundException {

		// empty file
		File file = new File(path);

		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8");
		PrintWriter pw = new PrintWriter(writer);
		for (String line : lines) {
			pw.write(line + "\n");
		}
		pw.close();
	}

	// This class splits a combined HL7 v2 message file, where messages are plainly concatenated.
	public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException, IOException {
		String sourceFilePath = args[0];
		String targetDirPath = args[1];

		MessageFileSplitter splitter = new MessageFileSplitter();
		splitter.execute(sourceFilePath, targetDirPath);

		System.out.println("DONE");
	}
}
