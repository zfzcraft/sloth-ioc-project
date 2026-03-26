package cn.zfzcraft.sloth.core.extension;

import java.lang.annotation.Annotation;

import cn.zfzcraft.sloth.core.factory.BeanFactory;

/**
 * must be no args constructor and @Extension
 */
public interface BeanFactoryAnnotationMatcher extends ExtensionPoint {

	Class<? extends BeanFactory> DEFAULT_NULL_FACTORY = null;

	/**
	 * for interface class ,must return a BeanFactoryClass
	 * 
	 * @return
	 */
	BeanFactory getBeanFactory();

	Class<? extends Annotation> getBeanAnnotationClass();
}
