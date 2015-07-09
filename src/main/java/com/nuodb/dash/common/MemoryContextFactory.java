package com.nuodb.dash.common;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * A basic initial context for in-memory structures.
 */
public class MemoryContextFactory implements InitialContextFactory {

    private static Context context;

    static {
        try {
            context = new InitialContext(true) {

                Map<String, Object> bindings = new HashMap<>();

                @Override
                public Object lookup(String name) throws NamingException {
                    return bindings.get(name);
                }

                @Override
                public void bind(String name, Object obj) throws NamingException {
                    bindings.put(name, obj);
                }
            };
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        return context;
    }
}
