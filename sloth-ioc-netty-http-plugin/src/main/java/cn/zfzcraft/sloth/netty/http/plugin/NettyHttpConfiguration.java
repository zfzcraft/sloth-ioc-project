package cn.zfzcraft.sloth.netty.http.plugin;

import cn.zfzcraft.sloth.annotations.Bean;
import cn.zfzcraft.sloth.annotations.Configuration;
import cn.zfzcraft.sloth.annotations.Eager;
import cn.zfzcraft.sloth.annotations.Imports;
import cn.zfzcraft.sloth.core.ApplicationContext;
import netty.http.mvc.NettyHttpServer;

@Configuration
@Imports(HttpProperties.class)
public class NettyHttpConfiguration {

	@Bean
	@Eager
	NettyHttpServer nettyHttpServer(ApplicationContext applicationContext, HttpProperties httpProperties) throws InterruptedException {
		return new NettyHttpServer(applicationContext, httpProperties);
	}
}
