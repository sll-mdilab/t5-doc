package net.sllmdilab.t5.converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import net.sll_mdilab.t5.CHAN;
import net.sll_mdilab.t5.EquipmentIdentifier;
import net.sll_mdilab.t5.Metric;
import net.sll_mdilab.t5.Metric.Facet;
import net.sll_mdilab.t5.Observation;
import net.sll_mdilab.t5.Observation.ObsIdentifier;
import net.sll_mdilab.t5.PCD01Message;
import net.sll_mdilab.t5.VMD;
import net.sllmdilab.commons.domain.SqlDevice;
import net.sllmdilab.commons.domain.SqlObservation;
import net.sllmdilab.commons.exceptions.T5Exception;

@Component
public class PCD_01MessageToSqlObservationsConverter {
	private static class IdItem {
		public final String level;
		public final String id;

		private IdItem(String level, String id) {
			this.level = level;
			this.id = id;
		}

		public static IdItem of(String level, String id) {
			return new IdItem(level, id);
		}

		public String toString() {
			return "(" + level + ", " + id + ")";
		}
	}

	public List<SqlObservation> convertToSqlObservations(Document document) {
		try {
			JAXBContext jaxbContext;
			jaxbContext = JAXBContext.newInstance(PCD01Message.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			PCD01Message message = (PCD01Message) unmarshaller.unmarshal(document);

			final List<SqlObservation> result = new ArrayList<>();

			message.getPatientResult().forEach(patientResult -> {
				patientResult.getOrderObservations().forEach(orderObs -> {
					orderObs.getMDS().forEach(mds -> {
						result.addAll(handleLevel("MDS", mds.getVMDAndObservation(), new LinkedList<>()));
					});
				});
			});

			return result;
		} catch (JAXBException e) {
			throw new T5Exception("Message parsing failed.", e);
		}
	}

	private List<SqlObservation> handleLevel(String level, List<Object> objects, Deque<IdItem> idStack) {
		List<SqlObservation> result = new LinkedList<>();

		List<IdItem> items = findIds(level, objects);
		items.forEach(item -> idStack.push(item));

		result.addAll(getSqlObservations(level, objects, idStack));

		objects.forEach(object -> {
			if (object instanceof VMD) {
				result.addAll(handleLevel("VMD", ((VMD) object).getCHANAndObservation(), idStack));
			} else if (object instanceof CHAN) {
				result.addAll(handleLevel("CHAN", ((CHAN) object).getMetricAndObservation(), idStack));
			} else if (object instanceof Metric) {
				result.addAll(handleLevel("Metric", ((Metric) object).getFacetAndObservation(), idStack));
			} else if (object instanceof Facet) {
				result.addAll(handleLevel("Facet", Arrays.asList(((Facet) object).getObservation()), idStack));
			}
		});

		for (int i = 0; i < items.size(); ++i) {
			idStack.pop();
		}

		return result;
	}

	private List<SqlObservation> getSqlObservations(String level, List<Object> objects, Deque<IdItem> idStack) {
		List<SqlObservation> result = new LinkedList<>();
		objects.forEach(object -> {
			if (object instanceof Observation) {
				Observation obs = (Observation) object;
				SqlObservation sqlObservation = new SqlObservation();

				if (obs.getValue() != null && !obs.getValue().isEmpty()) {
					sqlObservation.setValue(obs.getValue().get(0).getValue());
					sqlObservation.setValueType(obs.getValue().get(0).getTypeHL7V2());
				}

				for (ObsIdentifier identifier : obs.getObsIdentifier()) {
					if (identifier.isIsAlternate() != Boolean.TRUE) {
						sqlObservation.setCode(identifier.getValue());
						sqlObservation.setCodeSystem(identifier.getCodingSystemName());
						break;
					}
				}

				if (obs.getTimestamp() != null) {
					sqlObservation.setStartTime(obs.getTimestamp().getValue().toGregorianCalendar(TimeZone.getTimeZone("UTC"), null, null).getTime());
				}

				if (obs.getUnit() != null) {
					String value = obs.getUnit().getValue();
					if (!StringUtils.isBlank(value)) {
						sqlObservation.setUnit(value);
					}

					String system = obs.getUnit().getCodingSystemName();
					if (!StringUtils.isBlank(system)) {
						sqlObservation.setUnitSystem(system);
					}
				}

				sqlObservation.setSetId(obs.getSetid());

				sqlObservation.setUid(obs.getUid());

				sqlObservation.setDevices(createSqlDevices(idStack));

				result.add(sqlObservation);
			}
		});

		return result;
	}

	private List<SqlDevice> createSqlDevices(Deque<IdItem> items) {
		return items.stream().map(item -> {
			SqlDevice device = new SqlDevice();
			device.setDeviceId(item.id);
			device.setLevel(item.level);
			return device;
		}).collect(Collectors.toList());
	}

	private List<IdItem> findIds(String level, List<Object> objects) {
		List<IdItem> result = new ArrayList<>();
		objects.forEach(object -> {
			if (object instanceof Observation) {
				result.addAll(extractId(level, (Observation) object));
			}
		});

		return result;
	}

	private List<IdItem> extractId(String level, Observation obs) {
		List<IdItem> result = new ArrayList<>(1);
		EquipmentIdentifier equipmentIdentifier = obs.getEquipmentIdentifier();
		if (equipmentIdentifier != null) {
			String id = equipmentIdentifier.getValue();
			if (!StringUtils.isBlank(id)) {
				result.add(IdItem.of(level, id));
			}
		}

		return result;
	}
}
