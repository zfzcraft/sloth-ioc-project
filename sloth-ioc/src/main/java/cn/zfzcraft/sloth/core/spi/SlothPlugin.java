package cn.zfzcraft.sloth.core.spi;
import java.util.Set;

/**
 * must be no args constructor
 */
public interface SlothPlugin{
	
  void   registerBeanClasses(Set<Class<?>> pluginClasses);
    
}