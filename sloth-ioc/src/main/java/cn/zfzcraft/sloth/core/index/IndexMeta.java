package cn.zfzcraft.sloth.core.index;

import java.util.List;
import java.util.Set;

//-------------------------------------------------------------------------
// 数据模型：最终索引数据
// -------------------------------------------------------------------------
public class IndexMeta {
	private final Set<String> interfaces;
	private final List<String> annotations;
	private final boolean extendsPluginClass;

	public IndexMeta(Set<String> interfaces, List<String> annotations, boolean extendsPluginClass) {
		this.interfaces = interfaces;
		this.annotations = annotations;
		this.extendsPluginClass = extendsPluginClass;
	}

	public Set<String> getInterfaces() {
		return interfaces;
	}

	public List<String> getAnnotations() {
		return annotations;
	}

	public boolean isExtendsPluginClass() {
		return extendsPluginClass;
	}

	@Override
	public String toString() {
		return "IndexMeta{" + "interfaces=" + interfaces + ", annotations=" + annotations + ", extendsPluginClass="
				+ extendsPluginClass + '}';
	}
}