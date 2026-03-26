package cn.zfzcraft.sloth.core;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

public class BeanMetaInfo {

	private AnnotatedElement beanElement;
	private boolean eager;

	public BeanMetaInfo(AnnotatedElement beanElement, boolean eager) {
		super();

		this.beanElement = beanElement;
		this.eager = eager;
	}

	public boolean isEager() {
		return eager;
	}

	public AnnotatedElement getBeanElement() {
		return beanElement;
	}

	public boolean isClass() {
		return beanElement instanceof Class<?>;
	}

	public Class<?> getBeanClass() {
		return (Class<?>) beanElement;
	}

	public Method getBeanMethod() {
		return (Method) beanElement;
	}

}
