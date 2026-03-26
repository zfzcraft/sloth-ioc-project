package cn.zfzcraft.sloth.hikaricp.plugin;

import java.util.Set;

import cn.zfzcraft.sloth.core.spi.SlothPlugin;

public class HikariPlugin implements SlothPlugin {

	@Override
	public void registerBeanClasses(Set<Class<?>> classes) {
		classes.add(HikariConfiguration.class);

	}

}
