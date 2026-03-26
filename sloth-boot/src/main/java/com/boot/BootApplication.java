package com.boot;

import cn.zfzcraft.sloth.annotations.Bootstrap;
import cn.zfzcraft.sloth.core.BootstrapApplication;
@Bootstrap
public class BootApplication {

	public static void main(String[] args) {
		long begin = System.currentTimeMillis();
		BootstrapApplication.run(args, BootApplication.class);
		long end = System.currentTimeMillis();
		System.out.println("启动时间："+(end-begin));
	}
}
