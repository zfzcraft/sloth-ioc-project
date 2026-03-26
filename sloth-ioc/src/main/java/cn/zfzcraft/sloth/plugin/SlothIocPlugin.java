package cn.zfzcraft.sloth.plugin;

import java.util.Set;

import cn.zfzcraft.sloth.core.matcher.CompomentBeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.matcher.ConfigurationBeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.matcher.ConfigurationMethodBeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.matcher.ConfigurationPropertiesBeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.spi.SlothPlugin;

public class SlothIocPlugin implements SlothPlugin{

	@Override
	public void registerBeanClasses(Set<Class<?>> pluginClasses) {
		pluginClasses.add(CompomentBeanFactoryAnnotationMatcher.class);
		pluginClasses.add(ConfigurationBeanFactoryAnnotationMatcher.class);
		pluginClasses.add(ConfigurationMethodBeanFactoryAnnotationMatcher.class);
		pluginClasses.add(ConfigurationPropertiesBeanFactoryAnnotationMatcher.class);
	}

}
