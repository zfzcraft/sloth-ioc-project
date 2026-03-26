package cn.zfzcraft.sloth.core;

import java.lang.annotation.Annotation;
import java.util.List;


public interface ApplicationContext{

	Environment getEnvironment();
	
	<T> T getBean(Class<T> clazz);

	List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass);
	
	<T> List<Class<T>> getImplementationClasses(Class<T> interfaceClass);

}
