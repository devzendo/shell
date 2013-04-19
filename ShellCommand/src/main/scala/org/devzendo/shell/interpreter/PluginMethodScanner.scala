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

package org.devzendo.shell.interpreter

import org.apache.log4j.Logger
import java.lang.reflect.Method
import org.devzendo.shell.pipe.{InputPipe, OutputPipe}
import org.devzendo.shell.plugin.ShellPlugin

object PluginMethodScanner {
    private val LOGGER = Logger.getLogger(classOf[PluginMethodScanner])
    private val objectMethodNames = Set("getClass", "notify", "notifyAll",
        "wait", "equals", "hashCode", "toString")
    private val shellPluginMethodNames = Set("initialise")
}

class PluginMethodScanner {
    val methodAnalyser = new MethodAnalyser()

    def scanPluginMethods(shellPlugin: ShellPlugin): java.util.Map[String, AnalysedMethod] = {
        val returnMethods = new java.util.HashMap[String, AnalysedMethod]()
        val methods = shellPlugin.getClass.getMethods
        PluginMethodScanner.LOGGER.debug("Scanning " + methods.length + " method(s) from class " + shellPlugin.getClass.getSimpleName)
        val possiblePluginMethods = methods filter notObjectOrShellPluginMethodNames filter voidReturn filter validParameterTypes
        possiblePluginMethods.foreach(method => {
            PluginMethodScanner.LOGGER.debug("Considering method " + method)
            val optionalAnalysedMethod = methodAnalyser.analyseMethod(method)
            if (optionalAnalysedMethod.isDefined) {
                PluginMethodScanner.LOGGER.debug("Registering method " + method)
                returnMethods.put(method.getName, optionalAnalysedMethod.get)
            } else {
                PluginMethodScanner.LOGGER.debug("Not of the right signature")
            }
        })
        PluginMethodScanner.LOGGER.debug("Plugin scanned")
        returnMethods
    }
    
        
    private def notObjectOrShellPluginMethodNames(method: Method): Boolean = {
        val name = method.getName
        ! (PluginMethodScanner.objectMethodNames.contains(name) ||
            PluginMethodScanner.shellPluginMethodNames.contains(name))
    }

    private def voidReturn(method: Method): Boolean = {
        method.getReturnType.toString.equals("void") // no other way to detect this?
    }

    private def validParameterTypes(method: Method): Boolean = {
        method.getParameterTypes forall(c => {
            c == classOf[java.util.List[_]] ||
            c == classOf[InputPipe] ||
            c == classOf[OutputPipe]
        })
    }
}

class MethodAnalyser {
    def analyseMethod(method: Method): Option[AnalysedMethod] = {
        val analysedMethod = new AnalysedMethod(method)
        val parameterTypes = method.getParameterTypes
        if (parameterTypes.length == 0 ||
                
            (parameterTypes.length >= 1 && parameterTypes.length <= 3 &&
             (optionalInput(analysedMethod, parameterTypes) &&
              optionalOutput(analysedMethod, parameterTypes) &&
              optionalArguments(analysedMethod, parameterTypes)
             ))) {
            Option(analysedMethod)
        } else {
            None
        }
    }

    private def optionalArguments(analysedMethod: AnalysedMethod,
            parameterTypes: Array[Class[_]]): Boolean = {
        optionalParameter(parameterTypes, classOf[java.util.List[_]],
            (o: Option[Integer]) => analysedMethod.setArgumentsPosition(o))
    }
    
    private def optionalOutput(analysedMethod: AnalysedMethod,
            parameterTypes: Array[Class[_]]): Boolean = {
        optionalParameter(parameterTypes, classOf[OutputPipe], 
            (o: Option[Integer]) => analysedMethod.setOutputPipePosition(o))
    }

    private def optionalInput(analysedMethod: AnalysedMethod,
            parameterTypes: Array[Class[_]]): Boolean = {
        optionalParameter(parameterTypes, classOf[InputPipe], 
            (o: Option[Integer]) => analysedMethod.setInputPipePosition(o))
    }
    
    private def optionalParameter(parameterTypes: Array[Class[_]],
            searchClass: Class[_],
            storePosition: (Option[Integer]) => Unit): Boolean = {
        var count = 0
        var position: Option[Integer] = None
        for (i <- 0 until parameterTypes.length) {
            if (searchClass.isAssignableFrom(parameterTypes(i))) {
                position = Option(i)
                count = count + 1
            }
        }
        if (count == 0 || count == 1) {
            storePosition(position)
            return true
        }
        false
    }
}