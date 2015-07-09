package com.nuodb.dash.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Common utilities to load a resource off the classpath.
 */
public class Resources {

    public static Properties loadResource(Class<?> clazz, String path, Properties properties) throws IOException {
        try (InputStream in = clazz.getResourceAsStream(path)) {
            if (in != null) {
                properties.load(in);
            } else {
                throw new FileNotFoundException("Classpath resource not found: " + path);
            }
        }
        return properties;
    }

    public static StringBuilder loadResource(Class<?> clazz, String path, StringBuilder builder) throws IOException {
        try (InputStream in = clazz.getResourceAsStream(path)) {
            byte[] buffer = new byte[2048];
            int length;
            while ((length = in.read(buffer)) != -1) {
                builder.append(new String(buffer, 0, length));
            }
        }
        return builder;
    }

}
