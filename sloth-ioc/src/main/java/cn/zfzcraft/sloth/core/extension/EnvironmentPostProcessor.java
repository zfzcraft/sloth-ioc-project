package cn.zfzcraft.sloth.core.extension;

import cn.zfzcraft.sloth.core.Environment;
/**
 * must be no args constructor and @Extension
 */
public interface EnvironmentPostProcessor extends ExtensionPoint {
	
	void process(Environment environment);
	
	/**
	 * Smaller order executes earlier; larger order executes later.
	 * 
	 * @return
	 */
	int getOrder();

}
