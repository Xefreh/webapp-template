package org.example;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database implements AutoCloseable {

    private final HikariDataSource dataSource;

    public Database(AppConfig appConfig) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(appConfig.get("database.url"));
        hikariConfig.setUsername(appConfig.get("database.username"));
        hikariConfig.setPassword(appConfig.get("database.password"));
        hikariConfig.setSchema(appConfig.get("database.schema"));
        hikariConfig.setPoolName(appConfig.get("database.poolName"));
        hikariConfig.setConnectionTimeout(appConfig.getLong("database.connectionTimeout") * 1000);
        hikariConfig.setMaximumPoolSize(appConfig.getInt("database.maximumPoolSize"));
        hikariConfig.addDataSourceProperty("ApplicationName", appConfig.get("database.applicationName"));

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
