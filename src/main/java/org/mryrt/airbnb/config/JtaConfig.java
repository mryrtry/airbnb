package org.mryrt.airbnb.config;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class JtaConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Bean(destroyMethod = "close")
    @Primary
    public DataSource dataSource() {
        AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
        ds.setUniqueResourceName("airbnbXaDataSource");
        ds.setXaDataSourceClassName("org.postgresql.xa.PGXADataSource");

        Properties xaProps = new Properties();
        xaProps.setProperty("url", url);
        xaProps.setProperty("user", username);
        xaProps.setProperty("password", password);
        ds.setXaProperties(xaProps);

        ds.setMaxPoolSize(10);
        ds.setMinPoolSize(1);
        ds.setMaxIdleTime(60);
        return ds;
    }
}
