package cn.zfzcraft.sloth.core.matcher;

import java.lang.annotation.Annotation;

import cn.zfzcraft.sloth.annotations.Bean;
import cn.zfzcraft.sloth.annotations.Extension;
import cn.zfzcraft.sloth.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.factory.BeanFactory;
import cn.zfzcraft.sloth.core.factory.ConfigurationMethodBeanFactory;
@Extension
public class ConfigurationMethodBeanFactoryAnnotationMatcher implements BeanFactoryAnnotationMatcher{

	@Override
	public BeanFactory getBeanFactory() {
		return new ConfigurationMethodBeanFactory();
	}

	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return Bean.class;
	}

}
