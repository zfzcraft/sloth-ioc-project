package cn.zfzcraft.sloth.netty.http.plugin;

import java.util.Set;

import cn.zfzcraft.sloth.core.spi.SlothPlugin;

public class NettyHttpPlugin implements SlothPlugin{

	@Override
	public void registerBeanClasses(Set<Class<?>> classes) {
		classes.add(NettyHttpConfiguration.class);
		classes.add(NettyHttpFactoryBeanAnnotationMatcher.class);
	}

}
