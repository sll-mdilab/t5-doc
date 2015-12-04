package net.sllmdilab.t5.dao;

import java.io.UnsupportedEncodingException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;

import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;

import net.sllmdilab.commons.exceptions.DatabaseException;
import net.sllmdilab.commons.exceptions.T5Exception;
import net.sllmdilab.commons.util.T5FHIRUtils;

@Repository
public class PCD01MessageDao {
	private static final Logger logger = LoggerFactory.getLogger(PCD01MessageDao.class);

	public enum MessageType {
		T5_MESSAGE_XML, HL7V2_MESSAGE_XML, HL7V2_ACK_XML
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public long insert(Document message) {
		logger.debug("Inserting message by SQL");

		String insertQuery = "INSERT INTO t5_message ( time, content ) VALUES ( ?, XMLPARSE( DOCUMENT ? ))";

		String xmlContent;
		try {
			xmlContent = T5FHIRUtils.xmlToString(message);
		} catch (UnsupportedEncodingException | TransformerException e) {
			throw new T5Exception("Error deserializing message", e);
		}

		PreparedStatementCreatorFactory pscFactory = new PreparedStatementCreatorFactory(insertQuery,
				Types.TIMESTAMP, Types.VARCHAR);
		pscFactory.setReturnGeneratedKeys(true);
		pscFactory.setGeneratedKeysColumnNames("id");
		PreparedStatementCreator psc = pscFactory
				.newPreparedStatementCreator(Arrays.asList(new Date(), xmlContent));
		logger.debug("Writing message to db: " + xmlContent);

		KeyHolder messageKeyHolder = new GeneratedKeyHolder();
		if (jdbcTemplate.update(psc, messageKeyHolder) != 1) {
			throw new DatabaseException("Message insertion failed.");
		}

		return messageKeyHolder.getKey().longValue();
	}
}
