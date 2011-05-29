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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.devzendo.shell.pipe.InputPipe;
import org.devzendo.shell.pipe.OutputPipe;

public class PluginMethodScanner {
    private static final Logger LOGGER = Logger
            .getLogger(PluginMethodScanner.class);
    private static final scala.Option<Integer> none = scala.Option.apply(null);
    
    public static class AnalysedMethod {
        private final Method method;
        private scala.Option<Integer> argumentsPosition = none;
        private scala.Option<Integer> inputPipePosition = none;
        private scala.Option<Integer> outputPipePosition = none;
        
        public AnalysedMethod(Method method) {
            this.method = method;
        }
        public final Method getMethod() {
            return method;
        }
        public final scala.Option<Integer> getArgumentsPosition() {
            return argumentsPosition;
        }
        public final void setArgumentsPosition(scala.Option<Integer> argumentsPosition) {
            this.argumentsPosition = argumentsPosition;
        }
        public final scala.Option<Integer> getInputPipePosition() {
            return inputPipePosition;
        }
        public final void setInputPipePosition(scala.Option<Integer> inputPipePosition) {
            this.inputPipePosition = inputPipePosition;
        }
        public final scala.Option<Integer> getOutputPipePosition() {
            return outputPipePosition;
        }
        public final void setOutputPipePosition(scala.Option<Integer> outputPipePosition) {
            this.outputPipePosition = outputPipePosition;
        }
    }
    
    public Map<String, AnalysedMethod> scanPluginMethods(final ShellPlugin shellPlugin) {
        final Map<String, AnalysedMethod> returnMethods = new HashMap<String, AnalysedMethod>();
        final Method[] methods = shellPlugin.getClass().getMethods();
        LOGGER.debug("Scanning " + methods.length + " method(s) from class " + shellPlugin.getClass().getSimpleName());
        for (final Method method : methods) {
            // Ignore Object methods
            final String name = method.getName();
            if (name.equals("getClass") || name.equals("notify") ||
                name.equals("notifyAll") || name.equals("wait") ||
                name.equals("equals") || name.equals("hashCode") ||
                name.equals("toString")) {
                continue;
            }

            // Ignore ShellPlugin methods
            if (name.equals("initialise")) {
                continue;
            }

            LOGGER.debug("Considering method " + method);
            final Class<?> returnType = method.getReturnType();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final AnalysedMethod analysedMethod = new AnalysedMethod(method);
            if (voidness(returnType) && onlyExpectedInputs(parameterTypes) &&
                   ((parameterTypes.length == 0) ||
                    
                    ((parameterTypes.length >= 1 && parameterTypes.length <= 3) &&
                            (optionalInput(analysedMethod, parameterTypes) &&
                             optionalOutput(analysedMethod, parameterTypes) &&
                             optionalArguments(analysedMethod, parameterTypes)
                            ))
                    )) {
                LOGGER.debug("Registering method " + method);
                returnMethods.put(name, analysedMethod);
            } else {
                LOGGER.debug("Not of the right signature");
            }
        }
        LOGGER.debug("Plugin scanned");
        return returnMethods;
    }

    private boolean onlyExpectedInputs(Class<?>[] parameterTypes) {
        for (Class<?> parameterType : parameterTypes) {
            if (!parameterType.equals(List.class) && 
                !parameterType.equals(InputPipe.class) && 
                !parameterType.equals(OutputPipe.class)) {
                return false;
            }
        }
        return true;
    }

    private boolean optionalArguments(
            AnalysedMethod analysedMethod,
            Class<?>[] parameterTypes) {
        final Class searchClass = List.class;
        int count = 0;
        scala.Option<Integer> position = none;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.equals(searchClass)) {
                position = scala.Option.apply(i);
                count++;
            }
        }
        if (count == 0 || count == 1) {
            analysedMethod.setArgumentsPosition(position);
            return true;
        }
        return false;
    }

    private boolean optionalOutput(
            AnalysedMethod analysedMethod,
            Class<?>[] parameterTypes) {
        final Class searchClass = OutputPipe.class;
        int count = 0;
        scala.Option<Integer> position = none;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.equals(searchClass)) {
                position = scala.Option.apply(i);
                count++;
            }
        }
        if (count == 0 || count == 1) {
            analysedMethod.setOutputPipePosition(position);
            return true;
        }
        return false;
    }

    private boolean optionalInput(
            AnalysedMethod analysedMethod,
            Class<?>[] parameterTypes) {
        final Class searchClass = InputPipe.class;
        int count = 0;
        scala.Option<Integer> position = none;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.equals(searchClass)) {
                position = scala.Option.apply(i);
                count++;
            }
        }
        if (count == 0 || count == 1) {
            analysedMethod.setInputPipePosition(position);
            return true;
        }
        return false;
    }

    private boolean voidness(final Class<?> klass) {
        return klass.toString().equals("void"); // no other way to detect this? 
    }

    private boolean input(final Class<?> klass) {
        return klass.isAssignableFrom(InputPipe.class);
    }

    private boolean output(final Class<?> klass) {
        return klass.isAssignableFrom(OutputPipe.class);
    }

    private boolean list(final Class<?> klass) {
        return klass.isAssignableFrom(List.class);
    }
}
