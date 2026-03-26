package cn.zfzcraft.sloth.netty.http.plugin;

import java.lang.annotation.Annotation;

import cn.zfzcraft.sloth.annotations.Extension;
import cn.zfzcraft.sloth.core.extension.BeanFactoryAnnotationMatcher;
import cn.zfzcraft.sloth.core.factory.BeanFactory;
import cn.zfzcraft.sloth.core.factory.CompomentBeanFactory;
import netty.http.mvc.RestController;
@Extension
public class NettyHttpFactoryBeanAnnotationMatcher implements BeanFactoryAnnotationMatcher{

	
	@Override
	public Class<? extends Annotation> getBeanAnnotationClass() {
		return RestController.class;
	}

	@Override
	public BeanFactory getBeanFactory() {
		return new CompomentBeanFactory();
	}

}
