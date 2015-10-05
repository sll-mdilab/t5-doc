package net.sllmdilab.t5.processors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import net.sll_mdilab.t5.Observation;
import net.sllmdilab.t5.converters.T5XmlToRivtaConverter;
import net.sllmdilab.t5.dao.PCD01MessageDao;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import se.riv.clinicalprocess.healthcond.basic.getobservationsresponder.v1.GetObservationsResponseType;
import se.riv.clinicalprocess.healthcond.basic.getobservationsresponder.v1.GetObservationsType;
import se.riv.clinicalprocess.healthcond.basic.v1.CVType;
import se.riv.clinicalprocess.healthcond.basic.v1.IIType;
import se.riv.clinicalprocess.healthcond.basic.v1.ObservationGroupType;
import se.riv.clinicalprocess.healthcond.basic.v1.ObservationType;
import se.riv.clinicalprocess.healthcond.basic.v1.TimePeriodType;

@RunWith(MockitoJUnitRunner.class)
public class RivtaGetObservationsProcessorTest {
	private static final String MOCK_TIMESTAMP_START = "2015-12-12T12:12:12.121Z";
	private static final String MOCK_TIMESTAMP_END = "2015-12-12T12:15:12.121Z";
	private static final String MOCK_PATIENT_ID = "mock_patient_id";
	private static final String MOCK_OBS_TYPE_CODE = "mock_obs_type_code";

	@Mock
	private PCD01MessageDao dao;

	@Mock
	private T5XmlToRivtaConverter converter;

	@InjectMocks
	private RivtaGetObservationsProcessor processor;

	private CamelContext camelContext;

	@Before
	public void init() throws Exception {
		MockitoAnnotations.initMocks(this);
		camelContext = new DefaultCamelContext();
	}

	@Test
	public void resultIsReturned() throws Exception {
		List<Observation> mockObservations = new ArrayList<>();
		mockObservations.add(new Observation());

		ArgumentCaptor<Date> startDateArgCaptor = ArgumentCaptor.forClass(Date.class);
		ArgumentCaptor<Date> endDateArgCaptor = ArgumentCaptor.forClass(Date.class);
		when(dao.findByPatientIdTimeAndCode(any(), startDateArgCaptor.capture(), endDateArgCaptor.capture(), any()))
				.thenReturn(mockObservations);

		ObservationType rivtaObservation = new ObservationType();
		when(converter.getObservation(any())).thenReturn(rivtaObservation);

		Exchange exchange = createExchange();

		processor.process(exchange);

		assertEquals(parseDate(MOCK_TIMESTAMP_START), startDateArgCaptor.getValue());
		assertEquals(parseDate(MOCK_TIMESTAMP_END), endDateArgCaptor.getValue());

		GetObservationsResponseType response = (GetObservationsResponseType) exchange.getOut().getBody();

		List<ObservationGroupType> obsGroups = response.getObservationGroup();

		assertEquals(1, obsGroups.size());

		ObservationGroupType obsGroup = obsGroups.get(0);

		List<ObservationType> observations = obsGroup.getObservation();
		assertEquals(1, observations.size());

		assertEquals(MOCK_PATIENT_ID, obsGroup.getPatient().getId().getRoot());
	}

	private Exchange createExchange() {
		Exchange exchange = new DefaultExchange(camelContext);
		Message message = new DefaultMessage();
		GetObservationsType request = createRequest();
		message.setBody(request);
		exchange.setIn(message);
		return exchange;
	}

	private Date parseDate(String string) {
		return DatatypeConverter.parseDateTime(string).getTime();
	}

	private GetObservationsType createRequest() {
		GetObservationsType request = new GetObservationsType();
		IIType patientId = new IIType();
		patientId.setRoot(MOCK_PATIENT_ID);
		request.setPatientId(patientId);

		CVType mockObsType = new CVType();
		mockObsType.setCode(MOCK_OBS_TYPE_CODE);
		request.getObservationType().add(mockObsType);

		TimePeriodType mockTimePeriod = new TimePeriodType();
		mockTimePeriod.setStart(MOCK_TIMESTAMP_START);
		mockTimePeriod.setEnd(MOCK_TIMESTAMP_END);
		request.setTime(mockTimePeriod);
		return request;
	}
}
