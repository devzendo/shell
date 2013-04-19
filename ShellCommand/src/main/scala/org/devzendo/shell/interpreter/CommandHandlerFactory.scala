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

import org.devzendo.shell.plugin.ShellPlugin
import java.lang.reflect.InvocationTargetException

class CommandHandlerFactory {

    /**
     * Create a CommandHandler that adapts a Method to a CommandHandler.
     *
     * @param plugin the plugin instance containing the method to adapt to a
     * CommandHandler
     * @param analysedMethod the method to adapt, this has already been checked
     * by the PluginMethodScanner to conform to the CommandHandler pattern,
     * i.e. optional List<Object> for args, optional InputPipe, optional
     * OutputPipe, no other args, void return type. The position of these is
     * stored in the AnalysedMethod.
     * @return the CommandHandler
     */
    def createHandler(plugin: ShellPlugin, analysedMethod: AnalysedMethod): CommandHandler = {
        val method = analysedMethod.getMethod
        val handler = new CommandHandler(
            method.getName,
            analysedMethod.getArgumentsPosition,
            analysedMethod.getInputPipePosition,
            analysedMethod.getOutputPipePosition) {

            @throws[CommandExecutionException]
            override def execute() {
                try {
                    // TODO the size of the needed array can be precomputed and
                    // held by AnalysedMethod. This can then just create the
                    // array of the correct size and insert the elements
                    var argsMap = scala.collection.immutable.Map[Integer, AnyRef]()
                    for (pos <- analysedMethod.getArgumentsPosition)
                        argsMap += (pos -> getArgs)
                    for (pos <- analysedMethod.getInputPipePosition)
                        argsMap += (pos -> getInputPipe)
                    for (pos <- analysedMethod.getOutputPipePosition)
                        argsMap += (pos -> getOutputPipe)
                    val argsList = (0 until argsMap.size).map { argsMap(_) }
                    val array = argsList.toArray
                    method.invoke(plugin, array:_*) // :_* ensures varargs gets called correctly
                } catch {
                    case iae: IllegalArgumentException =>
                        throw new CommandExecutionException("Illegal arguments: " + iae.getMessage)
                    case e: IllegalAccessException =>
                        throw new CommandExecutionException("Illegal access: " + e.getMessage)
                    case ite: InvocationTargetException =>
                        throw new CommandExecutionException("Invocation target exception: " + ite.getMessage)
                }
            }
        }
        handler
    }
}