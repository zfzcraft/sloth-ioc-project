package cn.zfzcraft.sloth.core.extension;

import cn.zfzcraft.sloth.core.ApplicationContext;
/**
 * must be no args constructor and @Extension
 */
public interface BeanPostProcessor extends ExtensionPoint {
	
	
	boolean matche(Class<?> beanClass);
	

	Object process(ApplicationContext applicationContext,Class<?> beanName, Object bean);

	/**
	 * Smaller order executes earlier; larger order executes later.
	 * 
	 * @return
	 */
	int getOrder();

}
