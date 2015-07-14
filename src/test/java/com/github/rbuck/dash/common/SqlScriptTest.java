package com.github.rbuck.dash.common;

import com.github.rbuck.dash.services.cloud.BusinessServices;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class SqlScriptTest {
    @Test
    public void testIt() {
        String sqlFile = "mysql-dialect-install.sql";
        try {
            StringBuilder builder = Resources.loadResource(BusinessServices.class, sqlFile, new StringBuilder());
            SqlScript splitter = new SqlScript();
            final List<String> statements = splitter.split(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
