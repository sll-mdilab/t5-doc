package net.sllmdilab.t5.dao;

import java.util.Arrays;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import net.sllmdilab.commons.exceptions.DatabaseException;
import net.sllmdilab.t5.domain.SqlDevice;
import net.sllmdilab.t5.domain.SqlObservation;

@Repository
public class ObservationDao {
	private static final Logger logger = LoggerFactory.getLogger(ObservationDao.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public long insert(SqlObservation observation) {
		long obsId = insertObservation(observation);
		
		for (SqlDevice device : observation.getDevices()) {
			device.setObservationId(obsId);
			insertDevice(device, obsId);
		}
		return obsId;
	}

	private long insertObservation(SqlObservation observation) {
		//@formatter:off
		String insertObsQuery = 
			"INSERT INTO t5_observation ( " + 
			"message_id, " + 
			"uid, " + 
			"set_id, " + 
			"start_time, " + 
			"end_time, " + 
			"value, " + 
			"value_type, " + 
			"code, " + 
			"code_system, " + 
			"unit, " + 
			"unit_system, " + 
			"sample_rate, " + 
			"data_range ) "
			+ "VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ))";
		//@formatter:on

		logger.debug("Writing observation to db.");

		GeneratedKeyHolder obsKeyHolder = new GeneratedKeyHolder();
		if (jdbcTemplate.update(insertObsQuery, new Date(), obsKeyHolder) != 1) {
			PreparedStatementCreatorFactory pscFactory = new PreparedStatementCreatorFactory(insertObsQuery);
			//@formatter:off
			PreparedStatementCreator psc = pscFactory.newPreparedStatementCreator(Arrays.asList(
					observation.getMessageId(),
					observation.getUid(),
					observation.getSetId(),
					observation.getStartTime(),
					observation.getEndTime(),
					observation.getValue(),
					observation.getValueType(),
					observation.getCode(),
					observation.getCodeSystem(),
					observation.getUnit(),
					observation.getUnitSystem(),
					observation.getSampleRate(),
					observation.getDataRange()));
			//@formatter:on
			jdbcTemplate.update(psc, obsKeyHolder);
			
			
			throw new DatabaseException("Observation insertion failed, zero rows updated.");
		}
		
		return (long) obsKeyHolder.getKey();
	}
	

	private void insertDevice(SqlDevice device, long observationId) {
		String insertObsQuery = "INSERT INTO t5_device ( device_id, level, observation_id ) VALUES ( ?, ?, ?)";

		logger.debug("Writing device ID to db.");;
		if (jdbcTemplate.update(insertObsQuery, device.getDeviceId(), device.getLevel(), observationId) != 1) {
			throw new DatabaseException("Device ID insertion failed, zero rows updated.");
		}
	}
}
