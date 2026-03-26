package cn.zfzcraft.sloth.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import cn.zfzcraft.sloth.annotations.Bean;
import cn.zfzcraft.sloth.annotations.ConditionalOnClass;
import cn.zfzcraft.sloth.annotations.ConditionalOnMissingBean;
import cn.zfzcraft.sloth.annotations.ConditionalOnPropertity;
import cn.zfzcraft.sloth.annotations.Configuration;
import cn.zfzcraft.sloth.annotations.ConfigurationProperties;
import cn.zfzcraft.sloth.annotations.Eager;
import cn.zfzcraft.sloth.annotations.Extension;
import cn.zfzcraft.sloth.annotations.Imports;
import cn.zfzcraft.sloth.core.exception.BeanCreationFailedException;
import cn.zfzcraft.sloth.core.exception.BeanFactoryNotFoundException;
import cn.zfzcraft.sloth.core.exception.BeanNotExistException;
import cn.zfzcraft.sloth.core.exception.ExtensionCreationFailedException;
import cn.zfzcraft.sloth.core.exception.IgnoreException;
import cn.zfzcraft.sloth.core.exception.TooManyBeanFactoriesException;
import cn.zfzcraft.sloth.core.exception.TooManyBeansException;
import cn.zfzcraft.sloth.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.extension.BeanPostProcessor;
import cn.zfzcraft.sloth.core.extension.EnvironmentLoader;
import cn.zfzcraft.sloth.core.extension.EnvironmentPostProcessor;
import cn.zfzcraft.sloth.core.factory.BeanFactory;
import cn.zfzcraft.sloth.core.index.ClassAnnotationInterfaceIndexParser;
import cn.zfzcraft.sloth.core.index.FileAnnotationInterfaceIndexParser;
import cn.zfzcraft.sloth.core.index.ClassIndex;
import cn.zfzcraft.sloth.core.spi.SlothPlugin;
import cn.zfzcraft.sloth.utils.NestedMapUtils;
import cn.zfzcraft.sloth.utils.ResourceUtils;
import cn.zfzcraft.sloth.utils.ResourcesNotFoundException;
import cn.zfzcraft.sloth.utils.ClassLoaderUtils;

public final class AnnotationConfigApplicationContext implements LifeCycleApplicationContext {

	private static final String DOT_CLASS = ".class";

	private static final String META_INF_BEANS_INDEX = "META-INF/beans.index";

	private AtomicBoolean refresh = new AtomicBoolean(false);

	private AtomicBoolean preheated = new AtomicBoolean(false);

	private Class<?> maincClass;

	private String[] args;

	private Map<String, Object> env = new HashMap<>();

	private Set<Class<?>> pluginClasses = new HashSet<>();

	private Map<Class<? extends Annotation>, BeanFactory> beanFactoryMap = new HashMap<>();

	// 按 order 升序：越小越先
	private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

	private final Map<Class<?>, Object> singletonPool = new ConcurrentHashMap<>();

	private ClassIndex classIndex;

	private Environment environment = new LocalEnvironment(env);

	private Map<Class<?>, BeanMetaInfo> beanMetaInfoMap = new HashMap<>();

	@Override
	public void setMaincClass(Class<?> maincClass) {
		this.maincClass = maincClass;
	}

	@Override
	public void setArgs(String[] args) {
		this.args = args;
	}

	@Override
	public void refresh() {
		if (refresh.compareAndSet(false, true)) {

			loadPlugin();

			loadEnvironment();

			doEnvironmentLoader();

			doEnvironmentPostProcessor();

			collectBeanFactoryAnnotationMatchers();

			scanBeanClasses();

			collectBeanPostProcessors();

			registerBeanMetaInfo();

			registerApplicationContext();

			instantiateEagerBeans();

			System.out.println("启动容器成功............");

			asyncPreheatBeansAndClearResources();

			registerShutdownHook();
		}
	}

	private void asyncPreheatBeansAndClearResources() {
		String prefix = EnvironmentProperties.class.getAnnotation(ConfigurationProperties.class).prefix();
		EnvironmentProperties environmentProperties = NestedMapUtils.loadAs(env, prefix, EnvironmentProperties.class);
		if (environmentProperties.isPreheat()) {
			new Thread(() -> {
				preheatBeans();
				preheated.compareAndSet(false, true);
				clearResources();

			}).start();
		}
	}

	private void clearResources() {
		pluginClasses.clear();
		pluginClasses = null;
		beanFactoryMap.clear();
		beanFactoryMap = null;
		beanPostProcessors.clear();
		beanPostProcessors = null;
		classIndex = null;
		beanMetaInfoMap.clear();
		beanMetaInfoMap = null;
	}

	private void preheatBeans() {
		for (String className : classIndex.getAllApplicationBeanClassIndex()) {
			try {
				Class<?> clazz = Class.forName(className, false, ClassLoaderUtils.getClassLoader());
				getBean(clazz);
			} catch (ClassNotFoundException e) {
				throw new BeanCreationFailedException("create bean " + className + " failed!", e);
			}
		}

	}

	private void loadEnvironment() {
		Map<String, Object> loadMap = EnvironmentInitializer.initializeEnvironment(args);
		env.putAll(loadMap);
	}

	private void registerApplicationContext() {
		singletonPool.putIfAbsent(ApplicationContext.class, this);
	}

	private void doEnvironmentPostProcessor() {
		for (Class<?> clazz : pluginClasses) {
			if (EnvironmentPostProcessor.class.isAssignableFrom(clazz) && clazz.isAnnotationPresent(Extension.class)) {
				try {
					EnvironmentPostProcessor environmentPostProcessor = (EnvironmentPostProcessor) clazz
							.getConstructor().newInstance();
					environmentPostProcessor.process(environment);
				} catch (Exception e) {
					throw new ExtensionCreationFailedException("扩展点类[" + clazz.getName() + "]实例化失败，必须为无参构造器", e);
				}
			}
		}
	}

	private void doEnvironmentLoader() {
		List<Class<?>> list = pluginClasses.stream().filter(
				ele -> EnvironmentLoader.class.isAssignableFrom(ele) && ele.isAnnotationPresent(Extension.class))
				.collect(Collectors.toList());
		for (Class<?> clazz : list) {
			try {
				EnvironmentLoader loader = (EnvironmentLoader) clazz.getConstructor().newInstance();
				Map<String, Object> networkMap = loader.load(environment);
				EnvironmentInitializer.deepMerge(env, networkMap);
			} catch (Exception e) {
				throw new ExtensionCreationFailedException("扩展点类[" + clazz.getName() + "]实例化失败，必须为无参构造器", e);
			}
		}
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			destory();
		}));
	}

	private void collectBeanFactoryAnnotationMatchers() {
		for (Class<?> clazz : pluginClasses) {
			if (BeanFactoryAnnotationMatcher.class.isAssignableFrom(clazz)
					&& clazz.isAnnotationPresent(Extension.class)) {
				try {
					Constructor<?> constructor = clazz.getConstructor();
					BeanFactoryAnnotationMatcher beanFactoryAnnotationMatcher = (BeanFactoryAnnotationMatcher) constructor
							.newInstance();
					beanFactoryMap.putIfAbsent(beanFactoryAnnotationMatcher.getBeanAnnotationClass(),
							beanFactoryAnnotationMatcher.getBeanFactory());
				} catch (Exception e) {
					throw new ExtensionCreationFailedException("扩展点类[" + clazz.getName() + "]实例化失败，必须为无参构造器", e);
				}
			}
		}
	}

	private void collectBeanPostProcessors() {
		for (Class<?> clazz : pluginClasses) {
			if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
				try {
					Constructor<?> constructor = clazz.getConstructor();
					BeanPostProcessor beanPostProcessor = (BeanPostProcessor) constructor.newInstance();
					beanPostProcessors.add(beanPostProcessor);
				} catch (Exception e) {
					throw new ExtensionCreationFailedException("扩展点类[" + clazz.getName() + "]实例化失败，必须为无参构造器", e);
				}
			}
		}
		beanPostProcessors.sort(Comparator.comparingInt(BeanPostProcessor::getOrder));
	}

	private void scanBeanClasses() {
		if (ResourceUtils.exists(META_INF_BEANS_INDEX)) {
			scanBeanIndex();
		} else {
			scanPackage();
		}
	}

	private void scanBeanIndex() {
		URL url = ResourceUtils.getResource(META_INF_BEANS_INDEX);
		if (url == null) {
			throw new ResourcesNotFoundException("resource " + META_INF_BEANS_INDEX + " not found!");
		}
		try (InputStream is = url.openStream()) {
			byte[] bytes = is.readAllBytes();
			classIndex = FileAnnotationInterfaceIndexParser.parse(bytes, beanFactoryMap.keySet());
		} catch (IOException e) {
			throw new IgnoreException("ignore", e);
		}
	}

	private void scanPackage() {
		classIndex = ClassAnnotationInterfaceIndexParser.scanByPackage(maincClass.getPackageName());
	}

	private boolean isAnnotationClass(Class<?> clazz) {
		for (Class<? extends Annotation> annotationClass : beanFactoryMap.keySet()) {
			if (clazz.isAnnotationPresent(annotationClass)) {
				return true;
			}
		}
		return false;
	}

	private void loadPlugin() {
		Set<Class<?>> tempPluginClasses = new HashSet<>();
		ClassLoader classLoader = ClassLoaderUtils.getClassLoader();
		ServiceLoader<SlothPlugin> loader = ServiceLoader.load(SlothPlugin.class, classLoader);
		for (SlothPlugin plugin : loader) {
			plugin.registerBeanClasses(tempPluginClasses);
		}
		for (Class<?> loadClass : tempPluginClasses) {
			pluginClasses.add(loadClass);
			if (loadClass.isAnnotationPresent(Imports.class) && loadClass.isAnnotationPresent(Configuration.class)) {
				Imports imports = loadClass.getAnnotation(Imports.class);
				for (Class<?> clazz : imports.value()) {
					pluginClasses.add(clazz);
				}
			}
		}
	}

	private void registerBeanMetaInfo() {
		Set<String> classNameList = classIndex.getApplicationAnnotationBeanClassIndex()
				.remove(Configuration.class.getName());
		if (classNameList != null && classNameList.size() > 0) {
			for (String className : classNameList) {
				try {
					Class<?> clazz = Class.forName(className, false, ClassLoaderUtils.getClassLoader());
					registerBeanMetaInfo(clazz);
				} catch (Exception e) {
					throw new BeanNotExistException("Bean " + className + " Not Exist !");
				}
			}
			classIndex.getAllApplicationBeanClassIndex().removeAll(classNameList);
		}
		for (Class<?> clazz : pluginClasses) {
			registerBeanMetaInfo(clazz);
		}
	}

	private void registerBeanMetaInfo(Class<?> clazz) {
		if (isAnnotationClass(clazz)) {
			boolean eager = getEager(clazz);
			beanMetaInfoMap.putIfAbsent(clazz, new BeanMetaInfo(clazz, eager));
			if (clazz.isAnnotationPresent(Configuration.class)) {
				registerMethodBeanMetaInfo(clazz);
			}
		}

	}

	private void registerMethodBeanMetaInfo(Class<?> clazz) {
		for (Method beanMethod : clazz.getDeclaredMethods()) {
			if (hasCondition(beanMethod)) {
				if (isConditionTrue(beanMethod)) {
					registerMethodBeanMetaInfo(beanMethod);
				}
			} else {
				registerMethodBeanMetaInfo(beanMethod);
			}
		}
	}

	private void registerMethodBeanMetaInfo(Method beanMethod) {
		if (beanMethod.isAnnotationPresent(Bean.class)) {
			boolean eagerMethod = getEager(beanMethod);
			Class<?> returnType = beanMethod.getReturnType();
			beanMetaInfoMap.putIfAbsent(returnType, new BeanMetaInfo(beanMethod, eagerMethod));
		}
	}

	private boolean getEager(Method beanMethod) {
		if (beanMethod.isAnnotationPresent(Eager.class)) {
			return true;
		}
		return false;
	}

	private boolean getEager(Class<?> clazz) {
		if (clazz.isAnnotationPresent(Eager.class)) {
			return true;
		}
		return false;
	}

	private void instantiateEagerBeans() {
		for (Entry<Class<?>, BeanMetaInfo> entry : beanMetaInfoMap.entrySet()) {
			Class<?> key = entry.getKey();
			BeanMetaInfo value = entry.getValue();
			if (value.isEager()) {
				getBean(key);
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> clazz) {
		Object bean = singletonPool.get(clazz);
		if (bean != null) {
			return (T) bean;
		}
		synchronized (singletonPool) {
			if (bean == null) {
				bean = createBean(clazz);
				singletonPool.put(clazz, bean);
			}
		}
		return (T) bean;
	}

	private Object createBean(Class<?> clazz) {
		Object beanObject = null;
		if (clazz.isInterface()) {
			beanObject = createInterfaceBean(clazz);
		}
		if (beanObject == null) {
			beanObject = createApplicationBean(clazz);
		}
		if (beanObject == null) {
			beanObject = createPluginBean(clazz);
		}
		if (beanObject == null) {
			throw new BeanNotExistException("Bean " + clazz + " Not Exist!");
		}
		for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
			if (beanPostProcessor.matche(clazz)) {
				beanObject = beanPostProcessor.process(this, clazz, beanObject);
			}
		}
		ProxyContext.bind(clazz, beanObject);
		return beanObject;
	}

	private Object createApplicationBean(Class<?> clazz) {
		Object beanObject = null;
		if (classIndex.getAllApplicationBeanClassIndex().contains(clazz.getName())) {
			Annotation[] annotations = clazz.getAnnotations();
			BeanFactory beanFactory = getBeanFactory(annotations);
			beanObject = beanFactory.createBean(this, clazz);
		}
		return beanObject;
	}

	private Object createInterfaceBean(Class<?> clazz) {
		Object beanObject = null;
		Set<String> classes = classIndex.getApplicationInterfaceBeanClassIndex().get(clazz.getName());
		if (classes == null || classes.isEmpty()) {
			throw new BeanNotExistException("Bean:" + clazz.getName() + " Not Exist!");
		}
		if (classes.size() > 1) {
			throw new TooManyBeansException("Too Many Beans:" + clazz.getName());
		}
		String className = classes.iterator().next();
		try {
			Class<?> targetClass = Class.forName(className, false, ClassLoaderUtils.getClassLoader());
			Annotation[] annotations = targetClass.getAnnotations();
			BeanFactory beanFactory = getBeanFactory(annotations);
			beanObject = beanFactory.createBean(this, targetClass);
		} catch (ClassNotFoundException e) {
			throw new BeanCreationFailedException("create bean " + className + " failed!", e);
		}
		return beanObject;
	}

	private Object createPluginBean(Class<?> clazz) {
		Object beanObject = null;
		BeanMetaInfo metaInfo = beanMetaInfoMap.get(clazz);
		if (metaInfo != null) {
			if (metaInfo.isClass()) {
				Class<?> beanClass = metaInfo.getBeanClass();
				Annotation[] annotations = beanClass.getAnnotations();
				BeanFactory beanFactory = getBeanFactory(annotations);
				beanObject = beanFactory.createBean(this, beanClass);
			} else {
				Method beanMethod = metaInfo.getBeanMethod();
				Annotation[] annotations = beanMethod.getAnnotations();
				BeanFactory beanFactory = getBeanFactory(annotations);
				beanObject = beanFactory.createBean(this, beanMethod);
			}
		}
		return beanObject;
	}

	private BeanFactory getBeanFactory(Annotation[] annotations) {
		List<BeanFactory> beanFactories = new ArrayList<>();
		for (Annotation annotation : annotations) {
			Class<? extends Annotation> annotationClass = annotation.annotationType();
			BeanFactory beanFactory = beanFactoryMap.get(annotationClass);
			if (beanFactory != null) {
				beanFactories.add(beanFactory);
			}
		}
		if (beanFactories.isEmpty()) {
			throw new BeanFactoryNotFoundException("Not Found Bean Factory!");
		}
		if (beanFactories.size() > 1) {
			throw new TooManyBeanFactoriesException("Too Many Bean Factories!");
		}
		return beanFactories.get(0);
	}

	@Override
	public void destory() {
		for (Entry<Class<?>, Object> entry : singletonPool.entrySet()) {
			Object bean = entry.getValue();
			if (bean instanceof DisposableBean disposableBean) {
				disposableBean.destroy();
			}
		}
		singletonPool.clear();
	}

	/**
	 * 判断类是否存在（标准 JVM 方式：仅检查 .class 资源，不加载类）
	 * 
	 * @param className 全类名，如 com.zaxxer.hikari.HikariDataSource
	 * @return 存在返回 true，不存在 false
	 */
	private boolean isClassPresent(String className) {
		if (className == null || className.isBlank()) {
			return false;
		}
		String resourceName = className.replace('.', '/') + DOT_CLASS;
		return ResourceUtils.getResource(resourceName) != null;
	}

	private boolean matchesProperty(Map<String, Object> config, String propertyKey, Object havingValue) {
		if (config == null || config.isEmpty()) {
			return false;
		}
		Object actualValue = NestedMapUtils.getNestedValue(config, propertyKey);
		if (actualValue == null) {
			return false;
		}
		return isValueMatch(actualValue, havingValue);
	}

	private boolean isValueMatch(Object actual, Object expected) {
		if (Objects.equals(actual, expected)) {
			return true;
		}
		// 布尔宽松匹配：true/"true"/"TRUE"/"True" 都算 true
		if (expected instanceof Boolean) {
			String actualStr = actual.toString().trim().toLowerCase();
			return Boolean.parseBoolean(actualStr) == (Boolean) expected;
		}
		// 字符串忽略大小写匹配
		if (expected instanceof String && actual instanceof String) {
			return ((String) expected).equalsIgnoreCase((String) actual);
		}
		return false;
	}

	private boolean hasCondition(Method method) {
		if (method.isAnnotationPresent(ConditionalOnClass.class)) {
			return true;
		}
		if (method.isAnnotationPresent(ConditionalOnMissingBean.class)) {
			return true;
		}
		if (method.isAnnotationPresent(ConditionalOnPropertity.class)) {
			return true;
		}
		return false;
	}

	private boolean isConditionTrue(Method method) {
		if (method.isAnnotationPresent(ConditionalOnClass.class)) {
			ConditionalOnClass conditionalOnClass = method.getAnnotation(ConditionalOnClass.class);
			String className = conditionalOnClass.className();
			if (!isClassPresent(className)) {
				return false;
			}
		}
		if (method.isAnnotationPresent(ConditionalOnPropertity.class)) {
			ConditionalOnPropertity conditionalOnPropertity = method.getAnnotation(ConditionalOnPropertity.class);
			String key = conditionalOnPropertity.key();
			String value = conditionalOnPropertity.value();
			if (!matchesProperty(env, key, value)) {
				return false;
			}
		}
		if (method.isAnnotationPresent(ConditionalOnMissingBean.class)) {
			Class<?> key = method.getReturnType();
			if (key.isInterface()) {
				Set<String> classNames = classIndex.getApplicationInterfaceBeanClassIndex().get(key.getName());
				if (classNames != null && !classNames.isEmpty()) {
					return false;
				}
			} else {
				boolean exist = classIndex.getAllApplicationBeanClassIndex().contains(key.getName());
				if (exist) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Environment getEnvironment() {
		return environment;
	}

	@Override
	public List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotationClass) {
		List<Class<?>> classes = new ArrayList<>();
		if (preheated.get() == false) {
			Set<String> classNames = classIndex.getApplicationAnnotationBeanClassIndex().get(annotationClass.getName());
			for (String className : classNames) {
				try {
					Class<?> clazz = Class.forName(className, false, ClassLoaderUtils.getClassLoader());
					classes.add(clazz);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			for (Class<?> clazz : singletonPool.keySet()) {
				if (clazz.isAnnotationPresent(annotationClass)) {
					classes.add(clazz);
				}
			}
		}
		return classes;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<Class<T>> getImplementationClasses(Class<T> interfaceClass) {
		List<Class<T>> classes = new ArrayList<>();
		if (preheated.get() == false) {
			Set<String> classNames = classIndex.getApplicationInterfaceBeanClassIndex().get(interfaceClass.getName());
			Set<String> filterClassNames = classIndex.getExtendsPluginClassIndex();
			for (String className : classNames) {
				try {
					Class<T> clazz = (Class<T>) Class.forName(className, false, ClassLoaderUtils.getClassLoader());
					classes.add(clazz);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			for (String className : filterClassNames) {
				try {
					Class<?> clazz = Class.forName(className, false, ClassLoaderUtils.getClassLoader());
					if (interfaceClass.isAssignableFrom(clazz)) {
						classes.add((Class<T>) clazz);
					}
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		} else {
			for (Class<?> clazz : singletonPool.keySet()) {
				if (interfaceClass.isAssignableFrom(clazz)) {
					classes.add((Class<T>) clazz);
				}
			}
		}
		return classes;
	}

}