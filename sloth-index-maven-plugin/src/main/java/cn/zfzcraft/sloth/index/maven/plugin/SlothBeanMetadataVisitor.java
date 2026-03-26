package cn.zfzcraft.sloth.index.maven.plugin;


import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sloth IOC 业务Bean元数据读取器
 * 仅处理业务包内的有效Bean类，过滤所有无效类，读取非JDK运行时注解
 */
public class SlothBeanMetadataVisitor extends ClassVisitor {

    // JDK原生核心包前缀，用于过滤注解
    private static final List<String> JDK_CORE_PREFIXES = Arrays.asList(
            "java.", "sun.", "jdk.", "com.sun.", "org.openjdk."
    );

    private final String basePackage;
    private boolean isValidBusinessBean = false;
    private String beanFullClassName;
    private final List<String> retainedAnnotations = new ArrayList<>();

    public SlothBeanMetadataVisitor(String basePackage) {
        super(Opcodes.ASM9);
        this.basePackage = basePackage;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // 转换为全类名
        String fullClassName = name.replace('/', '.');

        // 1. 非业务包类，直接无效
        if (!fullClassName.startsWith(basePackage)) {
            this.isValidBusinessBean = false;
            return;
        }

        // 2. 过滤无效类：仅保留可实例化的正常业务类
        if (isInvalidClass(access, name, fullClassName)) {
            this.isValidBusinessBean = false;
            return;
        }

        // 3. 标记为有效业务Bean
        this.isValidBusinessBean = true;
        this.beanFullClassName = fullClassName;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        // 只处理有效Bean的运行时可见注解
        if (!isValidBusinessBean || !visible) {
            return super.visitAnnotation(descriptor, visible);
        }

        // 过滤JDK原生注解，其余全部保留
        String annotationFullClassName = Type.getType(descriptor).getClassName();
        boolean isJdkAnnotation = JDK_CORE_PREFIXES.stream().anyMatch(annotationFullClassName::startsWith);
        if (!isJdkAnnotation) {
            this.retainedAnnotations.add(annotationFullClassName);
        }

        return super.visitAnnotation(descriptor, visible);
    }

    /**
     * 过滤无效类
     */
    private boolean isInvalidClass(int access, String internalName, String fullClassName) {
        // 跳过接口、抽象类、枚举、注解、合成类、模块/包信息类
        if ((access & Opcodes.ACC_INTERFACE) != 0
                || (access & Opcodes.ACC_ABSTRACT) != 0
                || (access & Opcodes.ACC_ENUM) != 0
                || (access & Opcodes.ACC_ANNOTATION) != 0
                || (access & Opcodes.ACC_SYNTHETIC) != 0
                || internalName.contains("module-info")
                || internalName.contains("package-info")) {
            return true;
        }

        // 跳过匿名类、局部类、内部类（名字带$）
        if (internalName.contains("$")) {
            return true;
        }

        // 跳过JDK核心类
        return JDK_CORE_PREFIXES.stream().anyMatch(fullClassName::startsWith);
    }

    // ========== 对外暴露结果 ==========
    public boolean isValidBusinessBean() {
        return isValidBusinessBean && !retainedAnnotations.isEmpty();
    }

    public String getBeanFullClassName() {
        return beanFullClassName;
    }

    public List<String> getRetainedAnnotations() {
        return retainedAnnotations;
    }
}
