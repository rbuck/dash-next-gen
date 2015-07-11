package com.github.rbuck.dash.common;

import com.github.rbuck.retry.SqlTransactionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;

/**
 * Responsible for bootstrapping the whole Spring auto-wiring of data sources.
 */
public class DataSourceContext implements AutoCloseable, SqlTransactionContext {

    public static final String DS_CONTEXT_NAME = "java:comp/env/jdbc/DashDS";

    private final ConfigurableApplicationContext context;

    @Autowired
    public DataSource dataSource;

    public DataSourceContext() {
        context = createApplicationContext();
    }

    private ConfigurableApplicationContext createApplicationContext() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "com/github/rbuck/dash/services/spring/spring-ds.xml");
        context.getAutowireCapableBeanFactory().autowireBean(this);
        context.start();

        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put(Context.INITIAL_CONTEXT_FACTORY,
                    MemoryContextFactory.class.getName());
            InitialContext ctx = new InitialContext(env);
            ctx.bind(DS_CONTEXT_NAME, dataSource);
        } catch (NamingException e) {
            throw new Error(e);
        }

        return context;
    }

    @Override
    public void close() throws IOException {
        context.close();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
