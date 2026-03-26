package cn.zfzcraft.sloth.core.matcher;

import java.lang.annotation.Annotation;

import cn.zfzcraft.sloth.annotations.Compoment;
import cn.zfzcraft.sloth.annotations.Extension;
import cn.zfzcraft.sloth.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.factory.BeanFactory;
import cn.zfzcraft.sloth.core.factory.CompomentBeanFactory;
@Extension
public class CompomentBeanFactoryAnnotationMatcher implements BeanFactoryAnnotationMatcher{

	@Override
	public BeanFactory getBeanFactory() {
		return new CompomentBeanFactory();
	}

	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return Compoment.class;
	}

}
