package cn.zfzcraft.sloth.mybatisplus.plugin;

import java.lang.reflect.AnnotatedElement;

import org.apache.ibatis.session.SqlSessionFactory;

import cn.zfzcraft.sloth.annotations.Extension;
import cn.zfzcraft.sloth.core.ApplicationContext;
import cn.zfzcraft.sloth.core.factory.BeanFactory;
@Extension
public class MybatisPlusMapperBeanFactory implements BeanFactory{

	@Override
	public Object createBean(ApplicationContext applicationContext, AnnotatedElement beanElement) {
		Class<?> mapperClass = (Class<?>) beanElement;
		SqlSessionFactory factory = applicationContext.getBean(SqlSessionFactory.class);
		if (!factory.getConfiguration().hasMapper(mapperClass)) {
			factory.getConfiguration().addMapper(mapperClass);
		}
		return factory.openSession().getMapper(mapperClass);
	}

}
