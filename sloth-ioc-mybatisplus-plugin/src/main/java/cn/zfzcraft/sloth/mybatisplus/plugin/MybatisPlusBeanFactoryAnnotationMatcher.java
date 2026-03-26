package cn.zfzcraft.sloth.mybatisplus.plugin;

import java.lang.annotation.Annotation;

import org.apache.ibatis.annotations.Mapper;

import cn.zfzcraft.sloth.annotations.Extension;
import cn.zfzcraft.sloth.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.factory.BeanFactory;

@Extension
public class MybatisPlusBeanFactoryAnnotationMatcher implements BeanFactoryAnnotationMatcher {

	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return Mapper.class;
	}

	@Override
	public BeanFactory getBeanFactory() {
		return new MybatisPlusMapperBeanFactory();
	}
}
