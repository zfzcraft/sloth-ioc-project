package cn.zfzcraft.sloth.mybatisplus.plugin;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.Environment.Builder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;

import cn.zfzcraft.sloth.annotations.Bean;
import cn.zfzcraft.sloth.annotations.ConditionalOnMissingBean;
import cn.zfzcraft.sloth.annotations.Configuration;
@Configuration
public class MybatisPlusConfiguration {

	private static final String MASTER = "master";

	@ConditionalOnMissingBean
	@Bean
	public SqlSessionFactory sessionFactory(DataSource dataSource) {
		Builder builder = new Builder(MASTER);
		Environment environment = builder.dataSource(dataSource).transactionFactory(new JdbcTransactionFactory()).build();
		MybatisConfiguration config = new MybatisConfiguration(environment);
		MybatisSqlSessionFactoryBuilder mybatisSqlSessionFactoryBuilder = new MybatisSqlSessionFactoryBuilder();
		SqlSessionFactory sessionFactory =mybatisSqlSessionFactoryBuilder.build(config);
		return sessionFactory;

	}
}
