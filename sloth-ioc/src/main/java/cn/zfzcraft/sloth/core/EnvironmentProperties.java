package cn.zfzcraft.sloth.core;

import cn.zfzcraft.sloth.annotations.ConfigurationProperties;

@ConfigurationProperties(prefix = "env")
public class EnvironmentProperties {

	private String active;
	
	private boolean preheat = false;

	public String getActive() {
		return active;
	}

	public void setActive(String active) {
		this.active = active;
	}

	public boolean isPreheat() {
		return preheat;
	}

	public void setPreheat(boolean preheat) {
		this.preheat = preheat;
	}

	
	
}
