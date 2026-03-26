package cn.zfzcraft.sloth.core.index;

import org.objectweb.asm.ClassReader;
import cn.zfzcraft.sloth.core.exception.IgnoreException;
import cn.zfzcraft.sloth.utils.ResourceUtils;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassAnnotationInterfaceIndexParser {

	// 排除系统类
	private static final List<String> EXCLUDE_CLASS_PREFIX = List.of("java.", "javax.", "jdk.", "sun.");

	// -------------------------------------------------------------------------
	// 对外：按路径扫描
	// -------------------------------------------------------------------------
	public static ClassIndex scanByPath(String path, String... basePackages) throws Exception {
		// 业务基础包（仅用于判断是否继承非业务父类）
		Set<String> basePackageSet = new HashSet<>();
		basePackageSet.addAll(Arrays.asList(basePackages));
		Map<String, ClassMeta> classMap = new HashMap<>();
		File file = new File(path);
		if (file.isDirectory()) {
			scanDirectory(file, classMap);
		} else if (path.endsWith(".jar")) {
			try (JarFile jarFile = new JarFile(file)) {
				scanJar(jarFile, "", classMap);
			}
		} else if (file.isFile() && path.endsWith(".class")) {
			try (FileInputStream fis = new FileInputStream(file)) {
				parseClass(fis, classMap);
			}
		}
		Map<String, IndexMeta> map = writeIndexMeta(classMap, basePackageSet);
		ClassIndex fileIndex = toFileIndex(map);
		return fileIndex;
	}

	// -------------------------------------------------------------------------
	// 对外：按包名扫描
	// -------------------------------------------------------------------------
	public static ClassIndex scanByPackage(String... basePackages) {
		// 业务基础包（仅用于判断是否继承非业务父类）
		Set<String> basePackageSet = new HashSet<>();
		basePackageSet.addAll(Arrays.asList(basePackages));
		Map<String, ClassMeta> classMap = new HashMap<>();
		for (String pkg : basePackages) {
			String path = pkg.replace('.', '/');
			Enumeration<URL> resources = ResourceUtils.getResources(path);
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				String protocol = url.getProtocol();
				if ("file".equals(protocol)) {
					try {
						scanDirectory(new File(url.toURI()), classMap);
					} catch (Exception e) {
						throw new IgnoreException("ignore", e);
					}
				} else if ("jar".equals(protocol)) {
					try {
						JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
						scanJar(jarFile, path, classMap);
					} catch (Exception e) {
						throw new IgnoreException("ignore", e);
					}
				}
			}
		}
		Map<String, IndexMeta> map = writeIndexMeta(classMap, basePackageSet);
		ClassIndex fileIndex = toFileIndex(map);
		return fileIndex;
	}

	// -------------------------------------------------------------------------
	// 扫描目录
	// -------------------------------------------------------------------------
	private static void scanDirectory(File dir, Map<String, ClassMeta> classMap) {
		if (!dir.exists())
			return;
		File[] files = dir.listFiles();
		if (files == null)
			return;
		for (File file : files) {
			if (file.isDirectory()) {
				scanDirectory(file, classMap);
			} else if (file.getName().endsWith(".class")) {
				try (FileInputStream fis = new FileInputStream(file)) {
					parseClass(fis, classMap);
				} catch (Exception e) {
					throw new IgnoreException("ignore", e);
				}
			}
		}
	}

	// -------------------------------------------------------------------------
	// 扫描Jar包
	// -------------------------------------------------------------------------
	private static void scanJar(JarFile jarFile, String basePath, Map<String, ClassMeta> classMap) {
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();
			if (name.endsWith(".class") && !entry.isDirectory() && (basePath.isEmpty() || name.startsWith(basePath))) {
				try (InputStream is = jarFile.getInputStream(entry)) {
					parseClass(is, classMap);
				} catch (Exception e) {
					throw new IgnoreException("ignore", e);
				}
			}
		}
	}

	// -------------------------------------------------------------------------
	// 解析class文件
	// -------------------------------------------------------------------------
	private static void parseClass(InputStream in, Map<String, ClassMeta> classMap) throws IOException {
		ClassReader cr = new ClassReader(in);
		MetaClassVisitor visitor = new MetaClassVisitor();
		cr.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		String className = visitor.getClassName();
		boolean excludeClass = EXCLUDE_CLASS_PREFIX.stream().anyMatch(className::startsWith);
		if (excludeClass || className.contains("$"))
			return;
		classMap.put(className,
				new ClassMeta(className, visitor.getSuperClass(), visitor.getInterfaces(), visitor.getAnnotations()));
	}

	// -------------------------------------------------------------------------
	// ✅ 核心修复：收集【所有接口】（业务+插件，不再过滤）
	// 支持：类实现接口、接口继承接口、业务接口继承插件接口
	// -------------------------------------------------------------------------
	private static Set<String> collectAllApplicationInterfaces(String className, Map<String, ClassMeta> classMap,
			Set<String> visited) {
		Set<String> result = new LinkedHashSet<>();
		if (className == null || visited.contains(className)) {
			return result;
		}
		visited.add(className);
		ClassMeta meta = classMap.get(className);
		if (meta == null) {
			return result;
		}
		// 收集所有直接接口（无业务包过滤，插件接口也会收录）
		for (String itf : meta.getInterfaces()) {
			result.add(itf);
			// 递归处理接口的父接口
			result.addAll(collectAllApplicationInterfaces(itf, classMap, visited));
		}
		// 递归处理父类
		String superClass = meta.getSuperClass();
		if (!"java.lang.Object".equals(superClass)) {
			result.addAll(collectAllApplicationInterfaces(superClass, classMap, visited));
		}
		return result;
	}

	// -------------------------------------------------------------------------
	// 判断：是否继承非业务父类（仅这里使用业务包判断）
	// -------------------------------------------------------------------------
	private static boolean hasExtendsPluginClass(ClassMeta meta, Set<String> basePackageSet) {
		String superClass = meta.getSuperClass();
		if (superClass == null || "java.lang.Object".equals(superClass)) {
			return false;
		}
		return !isApplicationClass(superClass, basePackageSet);
	}

	// -------------------------------------------------------------------------
	// 判断类是否属于业务包
	// -------------------------------------------------------------------------
	private static boolean isApplicationClass(String className, Set<String> basePackageSet) {
		for (String pkg : basePackageSet) {
			if (className.startsWith(pkg)) {
				return true;
			}
		}
		return false;
	}

	// -------------------------------------------------------------------------
	// 生成最终索引元数据
	// -------------------------------------------------------------------------
	private static Map<String, IndexMeta> writeIndexMeta(Map<String, ClassMeta> classMap, Set<String> basePackageSet) {
		Map<String, IndexMeta> finalMap = new LinkedHashMap<>();
		for (Entry<String, ClassMeta> entry : classMap.entrySet()) {
			ClassMeta meta = entry.getValue();
			Set<String> allInterfaces = collectAllApplicationInterfaces(meta.getClassName(), classMap, new HashSet<>());
			boolean extendNonBiz = hasExtendsPluginClass(meta, basePackageSet);
			finalMap.put(entry.getKey(), new IndexMeta(allInterfaces, meta.getAnnotations(), extendNonBiz));
		}
		return finalMap;
	}

	private static ClassIndex toFileIndex(Map<String, ? extends IndexMeta> classMap) {
		Map<String, Set<String>> applicationInterfaceBeanClassIndex = new HashMap<>();
		Map<String, Set<String>> applicationAnnotationBeanClassIndex = new HashMap<>();
		Set<String> allApplicationBeanClassIndex = new HashSet<>();
		Set<String> extendsPluginClassIndex = new HashSet<>();
		for (Map.Entry<String, ? extends IndexMeta> entry : classMap.entrySet()) {
			String className = entry.getKey();
			allApplicationBeanClassIndex.add(className);
			IndexMeta meta = entry.getValue();
			if (meta.isExtendsPluginClass()) {
				extendsPluginClassIndex.add(className);
			}
			for (String anno : meta.getAnnotations()) {
				applicationAnnotationBeanClassIndex.computeIfAbsent(anno, k -> new HashSet<>()).add(className);
			}
			for (String iface : meta.getInterfaces()) {
				applicationInterfaceBeanClassIndex.computeIfAbsent(iface, k -> new HashSet<>()).add(className);
			}
		}
		ClassIndex fileIndex = new ClassIndex(applicationAnnotationBeanClassIndex, applicationInterfaceBeanClassIndex,
				allApplicationBeanClassIndex, extendsPluginClassIndex);

		return fileIndex;
	}

	// -------------------------------------------------------------------------
	// 测试
	// -------------------------------------------------------------------------
//	public static void main(String[] args) throws Exception {
//		FileIndex map = scanByPath(
//				"D:\\workspace-spring-tool-suite-4-4.24.0.RELEASE\\customer_tag_system\\target\\classes",
//				"com.chinalife.pomelo");
//		
//	}

}