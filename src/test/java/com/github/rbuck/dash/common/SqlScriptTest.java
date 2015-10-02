package com.github.rbuck.dash.common;

import com.github.rbuck.dash.services.cloud.BusinessServices;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class SqlScriptTest {
    @Test
    public void testIt() {
        String[] sqlFiles = {"mysql-dialect-install.sql", "nuodb-dialect-install.sql"};
        for (String sqlFile : sqlFiles) {
            try {
                StringBuilder builder = Resources.loadResource(BusinessServices.class, sqlFile, new StringBuilder());
                SqlScript splitter = new SqlScript();
                final List<String> statements = splitter.split(builder.toString());
                System.out.println("done");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
