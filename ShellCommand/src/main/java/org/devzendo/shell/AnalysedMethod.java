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

import scala.Option;

import java.lang.reflect.Method;

public class AnalysedMethod {
    private static final Option<Integer> none = Option.apply(null);

    private final Method method;
    private Option<Integer> argumentsPosition = none;
    private Option<Integer> inputPipePosition = none;
    private Option<Integer> outputPipePosition = none;
    
    public AnalysedMethod(Method method) {
        this.method = method;
    }
    public final Method getMethod() {
        return method;
    }
    public final Option<Integer> getArgumentsPosition() {
        return argumentsPosition;
    }
    public final void setArgumentsPosition(Option<Integer> argumentsPosition) {
        this.argumentsPosition = argumentsPosition;
    }
    public final Option<Integer> getInputPipePosition() {
        return inputPipePosition;
    }
    public final void setInputPipePosition(Option<Integer> inputPipePosition) {
        this.inputPipePosition = inputPipePosition;
    }
    public final Option<Integer> getOutputPipePosition() {
        return outputPipePosition;
    }
    public final void setOutputPipePosition(Option<Integer> outputPipePosition) {
        this.outputPipePosition = outputPipePosition;
    }
}