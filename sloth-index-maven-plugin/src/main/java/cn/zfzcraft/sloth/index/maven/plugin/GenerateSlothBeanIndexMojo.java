package cn.zfzcraft.sloth.index.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.AnnotationVisitor;

/**
 * Sloth IOC 业务包Bean索引生成Mojo 绑定到编译后、打包前的PROCESS_CLASSES阶段
 * 仅扫描指定业务包，完整覆盖继承链，生成META-INF/beans.index
 */
@Mojo(name = "generate-index", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateSlothBeanIndexMojo extends AbstractMojo {

	private static final String BEANS_INDEX = "beans.index";

	private static final String META_INF = "META-INF";

	private static final String COMMA = ",";

	private static final String COLON = ":";
	private static final String DOT_CLASS = ".class";

	private static final char POINT = '.';

	private static final String CLASS = DOT_CLASS;

	private static final String BOOTSTRAP_DESC = "Lcn/zfzcraft/sloth/annotations/Bootstrap;";

	// 键值分隔符：=
	public static final String KEY_VALUE_SEPARATOR = "=";

	int find = 0;

	/**
	 * 业务项目启动主类（全类名） 对应 MANIFEST.MF 中的 Start-Class
	 */
	private String startClass;

	/**
	 * 项目编译输出目录，Maven自动注入
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
	private File classesDirectory;

	@Override
	public void execute() throws MojoExecutionException {
		try {
			findBootstrap();
			// 校验classes目录
			if (!classesDirectory.exists() || !classesDirectory.isDirectory()) {
				getLog().info("Classes目录不存在，跳过Sloth IOC Bean索引生成");
				return;
			}
			getLog().info("开始生成Sloth IOC Bean索引，业务包：" + startClass.getClass().getPackageName());
			List<String> indexLines = scanByPath(classesDirectory, startClass.getClass().getPackageName());
			// 写入索引文件
			writeIndexToFile(indexLines);
			getLog().info("Sloth IOC Bean索引生成完成，共生成 " + indexLines.size() + " 条索引记录");

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	// 查找 @Bootstrap 启动类（复用原逻辑）
	private void findBootstrap() throws IOException {
		String classesDir = classesDirectory.getAbsolutePath();
		Files.walk(Paths.get(classesDir)).forEach(p -> {
			if (!p.toString().endsWith(CLASS))
				return;
			try {
				ClassReader cr = new ClassReader(Files.readAllBytes(p));
				cr.accept(new ClassVisitor(Opcodes.ASM9) {
					@Override
					public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
						if (BOOTSTRAP_DESC.equals(desc)) {
							startClass = cr.getClassName().replace('/', POINT);
							find++;
						}
						return null;
					}
				}, ClassReader.SKIP_CODE);
			} catch (Exception ignored) {
			}
		});
		if (find > 1) {
			throw new RuntimeException("Too Many Class Annotated  With @Bootstrap,Expected Exactly One!");
		}
		if (startClass == null) {
			throw new RuntimeException("No Class Annotated  With @Bootstrap Found!");
		}
	}

	// 排除系统类
	private static final List<String> EXCLUDE_CLASS_PREFIX = List.of("java.", "javax.", "jdk.", "sun.");

	// -------------------------------------------------------------------------
	// 对外：按路径扫描
	// -------------------------------------------------------------------------
	private List<String> scanByPath(File file, String... basePackages) throws Exception {
		// 业务基础包（仅用于判断是否继承非业务父类）
		Set<String> basePackageSet = new HashSet<>();
		basePackageSet.addAll(Arrays.asList(basePackages));
		Map<String, ClassMeta> classMap = new HashMap<>();
		if (file.isDirectory()) {
			scanDirectory(file, classMap);
		} else if (file.isFile() && file.getName().endsWith(".class")) {
			try (FileInputStream fis = new FileInputStream(file)) {
				parseClass(fis, classMap);
			}
		}
		Map<String, IndexMeta> map = writeIndexMeta(classMap, basePackageSet);
		return writeIndex(map);
	}

	// -------------------------------------------------------------------------
	// 扫描目录
	// -------------------------------------------------------------------------
	private void scanDirectory(File dir, Map<String, ClassMeta> classMap) throws Exception {
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
					throw e;
				}
			}
		}
	}

	// -------------------------------------------------------------------------
	// 解析class文件
	// -------------------------------------------------------------------------
	private void parseClass(InputStream in, Map<String, ClassMeta> classMap) throws IOException {
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
	private Set<String> collectAllApplicationInterfaces(String className, Map<String, ClassMeta> classMap,
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
	private boolean hasExtendsPluginClass(ClassMeta meta, Set<String> basePackageSet) {
		String superClass = meta.getSuperClass();
		if (superClass == null || "java.lang.Object".equals(superClass)) {
			return false;
		}
		return !isApplicationClass(superClass, basePackageSet);
	}

	// -------------------------------------------------------------------------
	// 判断类是否属于业务包
	// -------------------------------------------------------------------------
	private boolean isApplicationClass(String className, Set<String> basePackageSet) {
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
	private Map<String, IndexMeta> writeIndexMeta(Map<String, ClassMeta> classMap, Set<String> basePackageSet) {
		Map<String, IndexMeta> finalMap = new LinkedHashMap<>();
		for (Entry<String, ClassMeta> entry : classMap.entrySet()) {
			ClassMeta meta = entry.getValue();
			Set<String> allInterfaces = collectAllApplicationInterfaces(meta.getClassName(), classMap, new HashSet<>());
			boolean extendNonBiz = hasExtendsPluginClass(meta, basePackageSet);
			finalMap.put(entry.getKey(), new IndexMeta(allInterfaces, meta.getAnnotations(), extendNonBiz));
		}
		return finalMap;
	}

	private List<String> writeIndex(Map<String, IndexMeta> map) {
		StringBuilder stringBuilder = new StringBuilder();
		List<String> indexLines = new ArrayList<>();
		for (Entry<String, IndexMeta> entry : map.entrySet()) {
			IndexMeta indexMeta = entry.getValue();
			boolean isExtendsPluginClass = indexMeta.isExtendsPluginClass();
			String className = entry.getKey();
			List<String> annotations = indexMeta.getAnnotations();
			Set<String> interfaces = indexMeta.getInterfaces();
			for (String annotation : annotations) {
				stringBuilder.setLength(0);
				stringBuilder.append(annotation);
				stringBuilder.append(COLON);
				stringBuilder.append(className);
				stringBuilder.append(COLON);
				stringBuilder.append(String.join(COMMA, interfaces));
				stringBuilder.append(COLON);
				stringBuilder.append(isExtendsPluginClass);
				indexLines.add(stringBuilder.toString());
			}

		}

		return indexLines;

	}

	/**
	 * 写入索引文件到 META-INF/beans.index
	 */
	private void writeIndexToFile(List<String> indexLines) throws IOException {
		File metaInfDir = new File(classesDirectory, META_INF);
		if (!metaInfDir.exists()) {
			metaInfDir.mkdirs();
		}

		File indexFile = new File(metaInfDir, BEANS_INDEX);
		try (BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(Files.newOutputStream(indexFile.toPath()), StandardCharsets.UTF_8))) {
			for (String line : indexLines) {
				writer.write(line);
				writer.newLine();
			}
		}
	}

}