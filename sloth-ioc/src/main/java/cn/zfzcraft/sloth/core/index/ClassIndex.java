package cn.zfzcraft.sloth.core.index;

import java.util.Map;
import java.util.Set;

public class ClassIndex {

	private Map<String, Set<String>> applicationAnnotationBeanClassIndex;

	private Map<String, Set<String>> applicationInterfaceBeanClassIndex;

	private Set<String> allApplicationBeanClassIndex;
	
	private	Set<String> extendsPluginClassIndex;

	public ClassIndex(Map<String, Set<String>> applicationAnnotationBeanClassIndex,
			Map<String, Set<String>> applicationInterfaceBeanClassIndex, Set<String> allApplicationBeanClassIndex,Set<String> extendsPluginClassIndex) {
		super();
		this.applicationAnnotationBeanClassIndex = applicationAnnotationBeanClassIndex;
		this.applicationInterfaceBeanClassIndex = applicationInterfaceBeanClassIndex;
		this.allApplicationBeanClassIndex = allApplicationBeanClassIndex;
		this.extendsPluginClassIndex = extendsPluginClassIndex;
	}

	public Map<String, Set<String>> getApplicationAnnotationBeanClassIndex() {
		return applicationAnnotationBeanClassIndex;
	}

	public void setApplicationAnnotationBeanClassIndex(Map<String, Set<String>> applicationAnnotationBeanClassIndex) {
		this.applicationAnnotationBeanClassIndex = applicationAnnotationBeanClassIndex;
	}

	public Map<String, Set<String>> getApplicationInterfaceBeanClassIndex() {
		return applicationInterfaceBeanClassIndex;
	}

	public void setApplicationInterfaceBeanClassIndex(Map<String, Set<String>> applicationInterfaceBeanClassIndex) {
		this.applicationInterfaceBeanClassIndex = applicationInterfaceBeanClassIndex;
	}

	public Set<String> getAllApplicationBeanClassIndex() {
		return allApplicationBeanClassIndex;
	}

	public void setAllApplicationBeanClassIndex(Set<String> allApplicationBeanClassIndex) {
		this.allApplicationBeanClassIndex = allApplicationBeanClassIndex;
	}

	public Set<String> getExtendsPluginClassIndex() {
		return extendsPluginClassIndex;
	}

	public void setExtendsPluginClassIndex(Set<String> extendsPluginClassIndex) {
		this.extendsPluginClassIndex = extendsPluginClassIndex;
	}

}
