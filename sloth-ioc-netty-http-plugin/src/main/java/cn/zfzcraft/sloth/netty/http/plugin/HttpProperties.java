package cn.zfzcraft.sloth.netty.http.plugin;

import cn.zfzcraft.sloth.annotations.ConfigurationProperties;

@ConfigurationProperties(prefix = "netty")
public class HttpProperties {

	private int port;

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
