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

import java.util.HashMap;
import java.util.Map;

public class VariableRegistry {
    private Map<String, Variable> vars = new HashMap<String, Variable>();
    
    public boolean exists(VariableReference varRef) {
        synchronized (vars) {
            return vars.containsKey(varRef.variableName());
        }
    }

    public Variable getVariable(VariableReference varRef) {
        synchronized (vars) {
            final String varName = varRef.variableName();
            final Variable var = vars.get(varName);
            if (var == null) {
                final Variable newVar = new Variable();
                vars.put(varName, newVar);
                return newVar;
            }
            return var;
        }
    }

    public void setVariable(VariableReference varRef, Variable var) {
        synchronized (vars) {
            vars.put(varRef.variableName(), var);
        }        
    }
}
