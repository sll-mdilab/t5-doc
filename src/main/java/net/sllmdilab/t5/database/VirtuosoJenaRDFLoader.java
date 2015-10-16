package net.sllmdilab.t5.database;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import net.sllmdilab.commons.util.Constants;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.update.UpdateException;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

public class VirtuosoJenaRDFLoader {
	
	// Insert batches of max 1000 triples/lines
	private static final int BATCH_SIZE = 1000;
	private static final int QUERY_TIMEOUT_SECONDS = 2;

	private static Logger logger = LoggerFactory.getLogger(VirtuosoJenaRDFLoader.class);
	
	private String username;
	private String pw;
	private String url;
	private VirtGraph virtGraph;

	public VirtuosoJenaRDFLoader(String host, int port, String username, String pw) {
		this.username = username;
		this.pw = pw;
		this.url = "jdbc:virtuoso://" + host + ":" + port + "/charset=UTF-8/log_enable=2";
		
		if(!StringUtils.isBlank(host)) {
			connect();
		}
	}
	
	private void connect() {
		virtGraph = new VirtGraph(null, url, username, pw, true);
		virtGraph.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
		
		logger.info("Virtuoso connected to " + url);
	}

	public void close() {
		if (virtGraph != null) {
			virtGraph.close();
		}
		virtGraph = null;
	}

	public void loadNtriples(String triples, String graph) {

		// Add suffix to graph, to make it unique
		long now = System.currentTimeMillis();
		String unique_graph = graph + "_" + now;

		Date dtNow = new Date(now);
		String xmlNow = convertDateToXMLTypeWithZ(dtNow);
		
		// Timestamp triple of graph creation
		String tsTriple = "<" + unique_graph + "> <http://sll-mdilab.net/T5#time_created> '" + xmlNow + "'^^xsd:dateTime .";
		String queryTemplate = "INSERT INTO <" + unique_graph + ">\n{\n_TRIPLES_\n" + tsTriple + "\n}\nWHERE {}";

		long start = System.currentTimeMillis();

		String[] lines = triples.split("\n");
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			buf.append(lines[i]);
			buf.append("\n");

			if ((i % BATCH_SIZE == 0 && i > 0) || (i + 1) == lines.length) {
				String query = queryTemplate.replace("_TRIPLES_", buf.toString());
				
				try {
					sendUpdateRequest(query, virtGraph);
				} catch (UpdateException e) {
					// Retry once
					logger.info("Virtuoso connection failed, reconnecting...");
					connect();
					
					sendUpdateRequest(query, virtGraph);
				}
				
				// Re-init buffer
				buf = new StringBuilder();
			}
		}

		logger.info("loaded in:" + (System.currentTimeMillis() - start) + " msec");
	}
	
	private void sendUpdateRequest(String query, VirtGraph virtGraph) {
		long startTime = System.currentTimeMillis();
		VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, virtGraph);
		vur.exec();
		logger.info("Virtuoso request took " + (System.currentTimeMillis() - startTime) + " ms.");
	}

	private String convertDateToXMLTypeWithZ(Date date) {

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(Constants.ISO_DATE_FORMAT).withZone(
				ZoneId.of("UTC"));

		ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));

		return zonedDateTime.format(dateTimeFormatter) + "Z";
	}
}
