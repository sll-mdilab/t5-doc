package net.sllmdilab.t5.config;

import java.net.URI;
import java.net.URISyntaxException;

import javax.sql.DataSource;

import net.sllmdilab.commons.database.MLDBClient;
import net.sllmdilab.commons.exceptions.RosettaInitializationException;
import net.sllmdilab.commons.t5.validators.RosettaValidator;
import net.sllmdilab.t5.converters.PCD_01MessageToXMLConverter;
import net.sllmdilab.t5.converters.XMLToRDFConverter;
import net.sllmdilab.t5.processors.TimeAdjustmentProcessor;

import org.apache.camel.component.hl7.HL7DataFormat;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.dataformat.soap.SoapJaxbDataFormat;
import org.apache.camel.dataformat.soap.name.ServiceInterfaceStrategy;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import com.marklogic.xcc.exceptions.XccConfigException;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.conf.check.DefaultValidator;
import ca.uhn.hl7v2.conf.check.Validator;
import ca.uhn.hl7v2.conf.parser.ProfileParser;
import ca.uhn.hl7v2.conf.spec.RuntimeProfile;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;

@Configuration
public class ApplicationConfiguration extends CamelConfiguration {
	private static Logger logger = LoggerFactory.getLogger(ApplicationConfiguration.class);

	@Value("${JDBC_CONNECTION_STRING}")
	private String jdbcConnectionString;
	
	@Value("${T5_DATABASE_HOST}")
	private String databaseHost;

	@Value("${T5_DATABASE_PORT}")
	private String databasePort;

	@Value("${T5_DATABASE_USER}")
	private String databaseUser;

	@Value("${T5_DATABASE_PASSWORD}")
	private String databasePassword;

	@Value("${T5_DATABASE_XCC_PORT}")
	private String databaseXccPort;

	@Value("${T5_DATABASE_XCC_NAME}")
	private String databaseXccName;

	@Value("${T5_TIME_ADJUSTMENT_ENABLED}")
	private boolean timeAdjustmentEnabled;
	
	@Value("${T5_DEFAULT_TIME_ZONE}")
	private String timeZoneId;
	
	@Bean
	public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
		PropertyPlaceholderConfigurer placeholderConfigurer = new PropertyPlaceholderConfigurer();
		placeholderConfigurer.setSystemPropertiesModeName("SYSTEM_PROPERTIES_MODE_OVERRIDE");
		return placeholderConfigurer;
	}

	@Bean(name = "hl7encoder")
	public HL7MLLPNettyEncoderFactory hl7Encoder() {
		HL7MLLPNettyEncoderFactory factory = new HL7MLLPNettyEncoderFactory();
		factory.setConvertLFtoCR(true);
		return factory;
	}

	@Bean(name = "hl7decoder")
	public HL7MLLPNettyDecoderFactory hl7Decoder() {
		HL7MLLPNettyDecoderFactory factory = new HL7MLLPNettyDecoderFactory();
		factory.setConvertLFtoCR(true);
		return factory;
	}

	@Bean
	public DatabaseClient databaseClient() {
		logger.info("Using database " + databaseUser + "@" + databaseHost + ":" + databasePort);

		return DatabaseClientFactory.newClient(databaseHost, Integer.parseInt(databasePort), databaseUser,
				databasePassword, Authentication.DIGEST);
	}

	@Bean
	public XMLDocumentManager xmlDocumentManager() {
		return databaseClient().newXMLDocumentManager();
	}

	@Bean
	public HapiContext hapiContext() {
		return new DefaultHapiContext(new CanonicalModelClassFactory("2.6"));
	}

	@Bean
	public HL7DataFormat hl7DataFormat() {
		HL7DataFormat hl7DataFormat = new HL7DataFormat();
		hl7DataFormat.setHapiContext(hapiContext());
		hl7DataFormat.setValidate(false);
		return hl7DataFormat;
	}

	@Bean
	public RuntimeProfile runtimeProfile() throws Exception {
		return (new ProfileParser(false)).parseClasspath("PCD01_ORU_R01-profile.xml");
	}

	@Bean
	public Validator validator() {
		return new DefaultValidator();
	}

	@Bean
	public PCD_01MessageToXMLConverter pcd_01MessageToXMLConverter() {
		return new PCD_01MessageToXMLConverter();
	}

	@Bean
	public XMLToRDFConverter xmlToRDFConverter() {
		return new XMLToRDFConverter("http://sll-mdilab.net/T5");
	}

	@Bean
	public RosettaValidator rosettaValidator() throws RosettaInitializationException {
		return new RosettaValidator();
	}

	@Bean
	public ContentSource contentSource() throws URISyntaxException, XccConfigException {
		return ContentSourceFactory.newContentSource(new URI("xcc://" + databaseUser + ":" + databasePassword + "@"
				+ databaseHost + ":" + databaseXccPort + "/" + databaseXccName));
	}

	@Bean
	public MLDBClient mldbClient() throws XccConfigException, URISyntaxException {
		return new MLDBClient(contentSource());
	}

	@Bean(name = "rivtaObservationsDataFormat")
	public SoapJaxbDataFormat soapJaxbDataFormat() {
		return new SoapJaxbDataFormat("se.riv.clinicalprocess.healthcond.basic.getobservationsresponder.v1",
				new ServiceInterfaceStrategy(
						se.riv.clinicalprocess.healthcond.basic.getobservations.v1.rivtabp21.GetObservationsResponderInterface.class,
						false));
	}

	@Bean
	public DataSource dataSource() throws ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		return new DriverManagerDataSource(jdbcConnectionString);
	}
	
	@Bean
	public TimeAdjustmentProcessor timeAdjustmentProcessor() {
		return new TimeAdjustmentProcessor(timeAdjustmentEnabled, timeZoneId);
	}
	
	@Bean
	public JdbcTemplate jdbcTemplate() throws ClassNotFoundException {
		return new JdbcTemplate(dataSource());
	}
}
