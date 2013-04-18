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

package org.devzendo.shell

class VariableRegistry {
    private var vars = scala.collection.mutable.Map[String, Variable]()

    def exists(varRef: VariableReference): Boolean = {
        vars.synchronized {
            vars.contains(varRef.variableName)
        }
    }

    def getVariable(varRef: VariableReference): Variable = {
        vars.synchronized {
            val varName = varRef.variableName
            if (vars.contains(varName)) {
                vars(varName)
            } else {
                val newVar = new Variable()
                vars += (varName -> newVar)
                newVar
            }
        }
    }

    def setVariable(varRef: VariableReference, variable: Variable) {
        vars.synchronized {
            vars.put(varRef.variableName, variable)
        }
    }

    def getVariables: Map[String, List[AnyRef]] = {
        vars.synchronized {
            val mut = vars.map( (p: (String, Variable)) => (p._1, p._2.get))
            return Map.empty ++ mut // signature seems to mandate an immutable map
        }
    }
}
