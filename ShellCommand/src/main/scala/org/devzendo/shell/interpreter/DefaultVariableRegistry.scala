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
import org.apache.log4j.Logger
import java.util.concurrent.atomic.AtomicInteger

object DefaultVariableRegistry {
    private val LOGGER = Logger.getLogger(classOf[DefaultVariableRegistry])
    private val registryCount = new AtomicInteger()
    def getRegistryCount = {
        registryCount.get()
    }
}

class DefaultVariableRegistry(@scala.reflect.BeanProperty override val parentScope: Option[VariableRegistry]) extends VariableRegistry(parentScope) {
    private var vars = scala.collection.mutable.Map[String, Variable]()
    private val id = DefaultVariableRegistry.registryCount.incrementAndGet()
    private var usageCount = 0

    override def toString = {
        vars.synchronized {
            "'" + (if (parentScope.isEmpty) "parent " else "child ") + id + "' #vars " + vars.size + " #usage " + usageCount
        }
    }

    def exists(varRef: VariableReference): Boolean = {
        vars.synchronized {
            vars.contains(varRef.variableName) ||
                (parentScope map { _.exists(varRef) } getOrElse false)
        }
    }

    def getVariableInScopeHierarchy(varRef: VariableReference): Option[Variable] = {
        vars.synchronized {
            val varName = varRef.variableName
            if (vars.contains(varName)) {
                Some(vars(varName))
            } else {
                (parentScope map { _.getVariableInScopeHierarchy(varRef) } getOrElse None)
            }
        }
    }

    def getVariable(varRef: VariableReference): Variable = {
        vars.synchronized {
            val varName = varRef.variableName
            if (vars.contains(varName)) {
                vars(varName)
            } else {
                val parentVar = parentScope map { _.getVariableInScopeHierarchy(varRef) } getOrElse None
                parentVar.getOrElse {
                    val newVar = new Variable()
                    vars += (varName -> newVar)
                    newVar
                }
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
            val localMap = vars.map( (p: (String, Variable)) => (p._1, p._2.get))
            val parentMap = parentScope map { _.getVariables } getOrElse Map.empty
            // local takes precendence over parent, in case of shadowed variables
            return Map.empty ++ parentMap ++ localMap
        }
    }

    def close() {
        vars.synchronized {
            DefaultVariableRegistry.LOGGER.debug("Closing variable registry " + toString)
            vars.values.foreach( _.close() )
            vars.clear()
        }
    }

    def currentUsageCount() = {
        vars.synchronized {
            usageCount
        }
    }

    def incrementUsage() {
        vars.synchronized {
            usageCount = usageCount + 1
            DefaultVariableRegistry.LOGGER.debug("Variable registry usage count incremented in registry " + toString)
        }
    }

    def decrementUsage() {
        vars.synchronized {
            if (usageCount > 0) {
                usageCount = usageCount - 1
                DefaultVariableRegistry.LOGGER.debug("Variable registry usage count decremented in registry " + toString)
                if (usageCount == 0) {
                    close()
                }
            }
        }
    }
}
