package cn.zfzcraft.sloth.core.matcher;

import java.lang.annotation.Annotation;

import cn.zfzcraft.sloth.annotations.Configuration;
import cn.zfzcraft.sloth.annotations.Extension;
import cn.zfzcraft.sloth.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.factory.BeanFactory;
import cn.zfzcraft.sloth.core.factory.CompomentBeanFactory;
@Extension
public class ConfigurationBeanFactoryAnnotationMatcher implements BeanFactoryAnnotationMatcher{

	@Override
	public BeanFactory getBeanFactory() {
		return new CompomentBeanFactory();
	}

	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return Configuration.class;
	}

}
