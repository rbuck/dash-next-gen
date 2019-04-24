package com.github.rbuck.dash.common;

import java.io.IOException;
import java.util.Properties;

/**
 * Loads and caches SQL dialect specific resources.
 */
public class Dialect {

    private final String name;
    private final Properties properties;

    public Dialect(Class<?> clazz) throws IOException {
        this(clazz, System.getProperties().getProperty("dash.db.type", "nuodb"));
    }

    public Dialect(Class<?> clazz, String name) throws IOException {
        this.properties = new Properties(Resources.loadResource(clazz, name + "-dialect.properties", System.getProperties()));
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public java.util.Set<Object> getKeys() {
        return properties.keySet();
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public Properties getProperties() {
        return properties;
    }
}
