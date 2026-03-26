package cn.zfzcraft.sloth.core.extension;

import java.util.Map;

import cn.zfzcraft.sloth.core.Environment;
/**
 * must be no args constructor and @Extension
 */
public interface EnvironmentLoader extends ExtensionPoint{

	/**
	 * 
	 * @param local
	 * @return nested map
	 */
	Map<String, Object> load(Environment local);
}
