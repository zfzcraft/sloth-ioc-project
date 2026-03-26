package cn.zfzcraft.sloth.hikaricp.plugin;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import cn.zfzcraft.sloth.annotations.Bean;
import cn.zfzcraft.sloth.annotations.ConditionalOnMissingBean;
import cn.zfzcraft.sloth.annotations.Configuration;
import cn.zfzcraft.sloth.annotations.Imports;

@Configuration
@Imports(HikariProperties.class)
public class HikariConfiguration {

	@ConditionalOnMissingBean
	@Bean
	public DataSource dataSource(HikariProperties properties) {
		HikariDataSource ds = new HikariDataSource(properties);
		return ds;
	}
}
