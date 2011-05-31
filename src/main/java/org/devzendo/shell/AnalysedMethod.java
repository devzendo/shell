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

public class AnalysedMethod {
    private static final scala.Option<Integer> none = scala.Option.apply(null);

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