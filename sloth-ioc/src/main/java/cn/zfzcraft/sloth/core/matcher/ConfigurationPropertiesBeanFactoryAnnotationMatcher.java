package cn.zfzcraft.sloth.core.matcher;

import java.lang.annotation.Annotation;

import cn.zfzcraft.sloth.annotations.ConfigurationProperties;
import cn.zfzcraft.sloth.annotations.Extension;
import cn.zfzcraft.sloth.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.factory.BeanFactory;
import cn.zfzcraft.sloth.core.factory.ConfigurationPropertiesBeanFactory;
@Extension
public class ConfigurationPropertiesBeanFactoryAnnotationMatcher implements BeanFactoryAnnotationMatcher{

	@Override
	public BeanFactory getBeanFactory() {
		return new ConfigurationPropertiesBeanFactory();
	}

	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return ConfigurationProperties.class;
	}

}
