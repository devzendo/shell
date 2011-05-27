/**
 * Copyright (C) 2008-2011 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.devzendo.shell;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class PluginMethodScanner {
    private static final Logger LOGGER = Logger
            .getLogger(PluginMethodScanner.class);
    
    public Map<String, Method> scanPluginMethods(final ShellPlugin shellPlugin) {
        final Map<String, Method> returnMethods = new HashMap<String, Method>();
        final Method[] methods = shellPlugin.getClass().getMethods();
        LOGGER.debug("Scanning " + methods.length + " method(s) from class " + shellPlugin.getClass().getSimpleName());
        for (final Method method : methods) {
            // Ignore Object methods
            final String name = method.getName();
            if (name.equals("notify") ||
                name.equals("notifyAll") || name.equals("wait")) {
                continue;
            }

            // Ignore ShellPlugin methods
            if (name.equals("initialise")) {
                continue;
            }

            LOGGER.debug("Considering method " + method);
            final Class<?> returnType = method.getReturnType();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            if ((voidness(returnType) || iterator(returnType)) && 
                   ((parameterTypes.length == 0) || 
                    (parameterTypes.length == 1 && list(parameterTypes[0])) ||
                    (parameterTypes.length == 2 && list(parameterTypes[0]) && iterator(parameterTypes[1])))) {
                LOGGER.debug("Registering method " + method);
                returnMethods.put(name, method);
            }
        }
        return returnMethods;
    }

    private boolean voidness(final Class<?> klass) {
        return klass.toString().equals("void"); // no other way to detect this? 
    }

    private boolean iterator(final Class<?> klass) {
        return klass.isAssignableFrom(Iterator.class);
    }

    private boolean list(final Class<?> klass) {
        return klass.isAssignableFrom(List.class);
    }
}
