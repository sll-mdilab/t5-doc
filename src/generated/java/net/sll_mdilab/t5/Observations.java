package net.sll_mdilab.t5;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "observations")
@XmlAccessorType(XmlAccessType.FIELD)
public class Observations {

	@XmlElement(name="Observation")
	private List<Observation> observations = new ArrayList<>();

	public List<Observation> getObservations() {
		return observations;
	}

	public void setObservations(List<Observation> observations) {
		this.observations = observations;
	}
}