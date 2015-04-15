/**
 * Copyright (C) 2008-2012 Matt Gumbley, DevZendo.org <http://devzendo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.devzendo.shell.pipe

import org.devzendo.shell.interpreter.Variable

/**
 * Used to contain the output of sub-commands, to be used as input to their
 * enclosing command (as a Variable that can be directly 'got' from, rather than
 * as their input pipe).
 */
class AnonymousVariablePipe extends OutputPipe {
    val contents = new Variable()
    private val outputPipe = new VariableOutputPipe(contents)

    def setTerminated() {
        outputPipe.setTerminated()
    }

    def push(obj: AnyRef) {
        outputPipe.push(obj)
    }
}
