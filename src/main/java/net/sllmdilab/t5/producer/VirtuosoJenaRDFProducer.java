package net.sllmdilab.t5.producer;

import net.sllmdilab.t5.database.VirtuosoJenaRDFLoader;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.shared.JenaException;

public class VirtuosoJenaRDFProducer extends DefaultProducer {
	private static Logger logger = LoggerFactory.getLogger(VirtuosoJenaRDFProducer.class);

	private VirtuosoJenaRDFLoader rdfLoader;
	private final String graph;

	private String databaseHost;
	private int databasePort;
	private String databaseUser;
	private String databasePassword;

	public VirtuosoJenaRDFProducer(Endpoint endpoint, String graph, String databaseHost, int databasePort,
			String databaseUser, String databasePassword) {
		super(endpoint);

		this.graph = graph;
		this.databaseHost = databaseHost;
		this.databasePort = databasePort;
		this.databaseUser = databaseUser;
		this.databasePassword = databasePassword;
	}

	@Override
	public void process(Exchange exchange) throws Exception {
		if (rdfLoader != null) {
			String triples = (String) exchange.getIn().getBody();
			rdfLoader.loadNtriples(triples, graph);
		} else {
			logger.warn("RDF Loader not defined. Triples will not be saved.");
		}
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();

		logger.info("Starting producer.");
		try {
			rdfLoader = new VirtuosoJenaRDFLoader(databaseHost, databasePort, databaseUser, databasePassword);
		} catch (JenaException e) {
			logger.warn("Error when connecting to Virtuoso.");
		}
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();

		logger.info("Stopping producer.");

		rdfLoader.close();
	}
}
