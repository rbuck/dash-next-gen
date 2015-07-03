package com.nuodb.field.common;

import java.io.IOException;
import java.util.Properties;

import static com.nuodb.field.common.Resources.loadResource;
import static java.lang.System.getProperties;

/**
 * Loads and caches SQL dialect specific resources.
 */
public class Dialect {

    private final String name;
    private final Properties properties;

    public Dialect(Class<?> clazz) throws IOException {
        this(clazz, getProperties().getProperty("db.type", "nuodb"));
    }

    public Dialect(Class<?> clazz, String name) throws IOException {
        this.properties = new Properties(loadResource(clazz, name + "-dialect.properties", getProperties()));
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public java.util.Set<Object> getKeys() {
        return properties.keySet();
    }

    public String getSqlStatement(String key) {
        return properties.getProperty(key);
    }
}
