package cn.zfzcraft.sloth.core;

public interface LifeCycleApplicationContext extends ApplicationContext {
	
	

	void refresh();
	
	void destory();

	void setArgs(String[] args);

	void setMaincClass(Class<?> maincClass);
}
