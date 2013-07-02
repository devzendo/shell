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

package org.devzendo.shell.interpreter

import org.devzendo.shell.ast.VariableReference

abstract class VariableRegistry(@scala.reflect.BeanProperty val parentScope: Option[VariableRegistry]) {
    def exists(varRef: VariableReference): Boolean
    def getVariable(varRef: VariableReference): Variable
    def setVariable(varRef: VariableReference, variable: Variable)
    def getVariables: Map[String, List[AnyRef]]
    def getVariableInScopeHierarchy(varRef: VariableReference): Option[Variable]
    def close()
    def currentUsageCount(): Int
    def incrementUsage()
    def decrementUsage()
}

