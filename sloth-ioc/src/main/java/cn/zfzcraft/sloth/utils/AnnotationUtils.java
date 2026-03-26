package cn.zfzcraft.sloth.utils;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import cn.zfzcraft.sloth.core.exception.IgnoreException;

public class AnnotationUtils {

	public static boolean hasAnnotation(InputStream inputStream, List<Class<? extends Annotation>> beanAnnotationClasses) {
		List<String> annotationsList = new ArrayList<>();
		for (Class<? extends Annotation> beanAnnotationClass : beanAnnotationClasses) {
			String annotationDesc = "L" + beanAnnotationClass.getName().replace('.', '/') + ";";
			annotationsList.add(annotationDesc);
		}	
		final boolean[] found = { false };
		try {
			ClassReader cr = new ClassReader(inputStream);
			cr.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					if (annotationsList.contains(desc)) {
						found[0] = true;
					}
					return super.visitAnnotation(desc, visible);
				}
			}, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

			return found[0];
		} catch (Exception e) {
			throw new IgnoreException("ignore", e);
		}
	}
}