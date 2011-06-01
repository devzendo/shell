/**
 * Copyright (C) 2008-2010 Matt Gumbley, DevZendo.org <http://devzendo.org>
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
 
package org.devzendo.shell

import collection.JavaConversions._

import org.apache.log4j.Logger
import java.util.{Map, HashMap, List}
import java.lang.reflect.Method
import org.devzendo.shell.pipe.{InputPipe, OutputPipe}

object PluginMethodScannerS {
    private val LOGGER = Logger.getLogger(classOf[PluginMethodScannerS])
}
class PluginMethodScannerS {

    def scanPluginMethods(shellPlugin: ShellPlugin): Map[String, AnalysedMethod] = {
        val returnMethods = new HashMap[String, AnalysedMethod]()
        val methods = shellPlugin.getClass.getMethods()
        PluginMethodScannerS.LOGGER.debug("Scanning " + methods.length + " method(s) from class " + shellPlugin.getClass().getSimpleName());
        val possiblePluginMethods = methods filter validNames filter voidReturn filter validParameterTypes
        possiblePluginMethods.foreach ( m => {
            PluginMethodScannerS.LOGGER.debug("Considering method " + m)
            val analysedMethod = new AnalysedMethod(m)
            val parameterTypes = m.getParameterTypes
            if (parameterTypes.length == 0 ||
                    
                (parameterTypes.length >= 1 && parameterTypes.length <= 3 &&
                 (optionalInput(analysedMethod, parameterTypes) &&
                  optionalOutput(analysedMethod, parameterTypes) &&
                  optionalArguments(analysedMethod, parameterTypes)
                 ))) {
                PluginMethodScannerS.LOGGER.debug("Registering method " + m)
                returnMethods.put(m.getName, analysedMethod);
            } else {
                PluginMethodScannerS.LOGGER.debug("Not of the right signature");
            }
        })
        PluginMethodScannerS.LOGGER.debug("Plugin scanned");
        return returnMethods;
    }
    
    private def validNames(method: Method): Boolean = {
        // Ignore Object methods
        val name = method.getName()
        if (name.equals("getClass") || name.equals("notify") ||
            name.equals("notifyAll") || name.equals("wait") ||
            name.equals("equals") || name.equals("hashCode") ||
            name.equals("toString")) {
            return false
        }

        // Ignore ShellPlugin methods
        if (name.equals("initialise")) {
            return false
        }

        return true
    }

    private def voidReturn(method: Method): Boolean = {
        method.getReturnType.toString().equals("void") // no other way to detect this? 
    }

    private def validParameterTypes(method: Method): Boolean = {
        method.getParameterTypes forall(c => {
            classOf[List[_]].isAssignableFrom(c) ||
            classOf[InputPipe].isAssignableFrom(c) ||
            classOf[OutputPipe].isAssignableFrom(c)
        })
    }

    private def optionalArguments(analysedMethod: AnalysedMethod,
            parameterTypes: Array[Class[_]]): Boolean = {
        val searchClass = classOf[List[_]]
        var count = 0
        var position: Option[Integer] = None
        for (i <- 0 until parameterTypes.length) {
            val parameterType = parameterTypes(i)
            if (searchClass.isAssignableFrom(parameterType)) {
                position = Option(i)
                count = count + 1
            }
        }
        if (count == 0 || count == 1) {
            analysedMethod.setArgumentsPosition(position)
            return true
        }
        return false
    }
    
    private def optionalOutput(analysedMethod: AnalysedMethod,
            parameterTypes: Array[Class[_]]): Boolean = {
        val searchClass = classOf[OutputPipe]
        var count = 0
        var position: Option[Integer] = None
        for (i <- 0 until parameterTypes.length) {
            val parameterType = parameterTypes(i)
            if (searchClass.isAssignableFrom(parameterType)) {
                position = Option(i)
                count = count + 1
            }
        }
        if (count == 0 || count == 1) {
            analysedMethod.setOutputPipePosition(position)
            return true
        }
        return false
    }

    private def optionalInput(analysedMethod: AnalysedMethod,
            parameterTypes: Array[Class[_]]): Boolean = {
        val searchClass = classOf[InputPipe]
        var count = 0
        var position: Option[Integer] = None
        for (i <- 0 until parameterTypes.length) {
            val parameterType = parameterTypes(i)
            if (searchClass.isAssignableFrom(parameterType)) {
                position = Option(i)
                count = count + 1
            }
        }
        if (count == 0 || count == 1) {
            analysedMethod.setInputPipePosition(position)
            return true
        }
        return false
    }
}