package cn.zfzcraft.sloth.core.index;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class FileAnnotationInterfaceIndexParser {

	/**
	 * 从索引文件流解析为： 0 -> annotationToClasses 1 -> interfaceToClasses
	 * 
	 * @param annotations
	 */
	public static ClassIndex parse(InputStream inputStream, List<String> annotationNames) {
		Map<String, Set<String>> annotationIndex = new HashMap<>();
		Map<String, Set<String>> interfaceIndex = new HashMap<>();
		Set<String> allClassIndex = new HashSet<>();
		Set<String> extendsPluginClassIndex = new HashSet<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();

				// 跳过注释/空行
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				// 按格式切分： annotation:className:interfaces:extendsPluginClass
				String[] parts = line.split(":", 4);
				if (parts.length < 4) {
					continue;
				}

				String annotation = parts[0].trim();
				if (annotationNames.contains(annotation)) {
					String className = parts[1].trim();
					String interfaceStr = parts[2].trim();
					boolean extendsPluginClass = Boolean.parseBoolean(parts[3].trim());
					if (extendsPluginClass) {
						extendsPluginClassIndex.add(className);
					}
					allClassIndex.add(className);
					// ------------ 注解 → 类 ------------
					if (!annotation.isBlank()) {
						annotationIndex.computeIfAbsent(annotation, k -> new HashSet<>()).add(className);
					}

					// ------------ 接口 → 实现类 ------------
					if (!interfaceStr.isBlank()) {
						String[] interfaces = interfaceStr.split(",");
						for (String iface : interfaces) {
							String itf = iface.trim();
							if (!itf.isBlank()) {
								interfaceIndex.computeIfAbsent(itf, k -> new HashSet<>()).add(className);
							}
						}
					}
				}

			}

		} catch (Exception e) {
			throw new RuntimeException("索引文件解析失败", e);
		}

		// 返回 [注解索引, 接口索引]
		return new ClassIndex(annotationIndex, interfaceIndex, allClassIndex,extendsPluginClassIndex);
	}

	/**
	 * 如果你想传 byte[]，也给你封装好
	 * 
	 * @param set
	 */
	public static ClassIndex parse(byte[] bytes, Set<Class<? extends Annotation>> annotations) {
		List<String> annotationNames = annotations.stream().map(ele -> ele.getName()).collect(Collectors.toList());
		return parse(new java.io.ByteArrayInputStream(bytes), annotationNames);
	}
}