package cn.zfzcraft.sloth.utils;

public class ClassLoaderUtils {
	

	public static ClassLoader getClassLoader() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = ClassLoaderUtils.class.getClassLoader();
		}
		return classLoader;
	}

	
}
