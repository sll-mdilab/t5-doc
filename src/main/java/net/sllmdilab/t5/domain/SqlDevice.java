package net.sllmdilab.t5.domain;

public class SqlDevice {
	private String deviceId;
	private String level;
	private long observationId;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public long getObservationId() {
		return observationId;
	}

	public void setObservationId(long observationId) {
		this.observationId = observationId;
	}
}
