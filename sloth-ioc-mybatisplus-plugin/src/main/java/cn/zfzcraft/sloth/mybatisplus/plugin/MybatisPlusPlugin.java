package cn.zfzcraft.sloth.mybatisplus.plugin;

import java.util.Set;

import cn.zfzcraft.sloth.core.spi.SlothPlugin;

public class MybatisPlusPlugin implements SlothPlugin {

	@Override
	public void registerBeanClasses(Set<Class<?>> classes) {
		classes.add(MybatisPlusConfiguration.class);
		classes.add(MybatisPlusMapperBeanFactory.class);
		classes.add(MybatisPlusBeanFactoryAnnotationMatcher.class);
	}

}
