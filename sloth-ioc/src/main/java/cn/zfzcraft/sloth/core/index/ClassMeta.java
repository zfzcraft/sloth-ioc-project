package cn.zfzcraft.sloth.core.index;

import java.util.List;

//-------------------------------------------------------------------------
// 数据模型：Class原始元数据
// -------------------------------------------------------------------------
public class ClassMeta {
	private final String className;
	private final String superClass;
	private final List<String> interfaces;
	private final List<String> annotations;

	public ClassMeta(String className, String superClass, List<String> interfaces, List<String> annotations) {
		this.className = className;
		this.superClass = superClass;
		this.interfaces = interfaces;
		this.annotations = annotations;
	}

	public String getClassName() {
		return className;
	}

	public String getSuperClass() {
		return superClass;
	}

	public List<String> getInterfaces() {
		return interfaces;
	}

	public List<String> getAnnotations() {
		return annotations;
	}
}