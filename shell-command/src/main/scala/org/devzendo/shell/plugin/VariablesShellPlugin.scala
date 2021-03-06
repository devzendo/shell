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

package org.devzendo.shell.plugin

import org.devzendo.shell.pipe.OutputPipe
import org.devzendo.shell.interpreter.VariableRegistry

class VariablesShellPlugin extends AbstractShellPlugin {
    def getName = "Variables"

    def listVariables(variableRegistry: VariableRegistry, outputPipe: OutputPipe) {
        val varMap: Map[String, List[AnyRef]] = variableRegistry.getVariables
        for (varEntry <- varMap) {
            outputPipe.push(varEntry._1 + "=" + varEntry._2)
        }
    }
}
