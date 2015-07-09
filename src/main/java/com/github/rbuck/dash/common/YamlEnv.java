package com.github.rbuck.dash.common;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Loads a configuration file that supports a custom env tag.
 */
public class YamlEnv {

    private final Tag envTag = new Tag("!env");

    private class EnvironmentConstructor extends Constructor {

        private class EnvConstruct extends AbstractConstruct {

            private Object mapValue(String value) {
                if (TypeValidator.isNull(value)) {
                    return null;
                }
                if (TypeValidator.isBoolean(value)) {
                    return Boolean.valueOf(value);
                }
                if (TypeValidator.isInteger(value)) {
                    return Integer.valueOf(value);
                }
                if (TypeValidator.isLong(value)) {
                    return Long.valueOf(value);
                }
                if (TypeValidator.isDouble(value)) {
                    return Double.valueOf(value);
                }
                return null;
            }

            @Override
            public Object construct(Node node) {
                if (node == null || !(node instanceof ScalarNode)) {
                    throw new IllegalArgumentException("Node not Scalar Node");
                }
                String val = (String) constructScalar((ScalarNode) node);
                val = val.substring(2, val.length() - 1);
                String[] terms = val.split(":");
                assert terms.length <= 2;
                //System.out.println(val);
                switch (terms.length) {
                    case 1: {
                        return mapValue(System.getProperty(terms[0]));
                    }
                    default: {
                        return mapValue(System.getProperty(terms[0], terms[1]));
                    }
                }
            }
        }

        public EnvironmentConstructor() {
            this.yamlConstructors.put(envTag, new EnvConstruct());
        }

        @Override
        protected Construct getConstructor(Node node) {
            if (envTag.equals(node.getTag())) {
                node.setUseClassConstructor(Boolean.FALSE);
            }
            return super.getConstructor(node);
        }
    }

    EnvironmentConstructor environment;
    Yaml yaml;

    public YamlEnv() {
        environment = new EnvironmentConstructor();
        yaml = new Yaml(environment);
        yaml.addImplicitResolver(envTag, Pattern.compile("\\$\\([a-zA-Z\\d\\u002E\\u005F]+(:[a-zA-Z\\d\\u002E\\u005F]+)?\\)"), "$");
    }

    public Object loadProperties(String filePath) throws IOException {
        return loadProperties(new File(filePath));
    }

    public Object loadProperties(File file) throws IOException {
        try (InputStream ios = new FileInputStream(file)) {
            return loadProperties(ios);
        }
    }

    public Object loadProperties(InputStream inputStream) {
        return yaml.load(inputStream);
    }
}
