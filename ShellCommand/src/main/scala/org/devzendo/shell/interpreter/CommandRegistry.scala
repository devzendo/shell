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
import org.devzendo.shell._
import scala.Some

class CommandRegistry {
    private val commandHandlerFactory = new CommandHandlerFactory
    private case class PluginMethod(shellPlugin: ShellPlugin, analysedMethod: AnalysedMethod) { }
    private var nameToPluginMethod = scala.collection.mutable.Map[String, PluginMethod]()

    @throws[DuplicateCommandException]
    def registerCommand(name: String, plugin: ShellPlugin, analysedMethod: AnalysedMethod) {
        nameToPluginMethod.get(name) match {
            case None =>
                nameToPluginMethod += (name -> new PluginMethod(plugin, analysedMethod))
            case Some(pluginMethod) =>
                throw new DuplicateCommandException("Command '" + name + "' from plugin '"
                    + plugin.getName + "' is duplicated; initially declared in plugin '"
                    + pluginMethod.shellPlugin.getName + "'");
        }
    }

    @throws[CommandNotFoundException]
    def getHandler(name: String): CommandHandler = {
        nameToPluginMethod.get(name) match {
            case None =>
                throw new CommandNotFoundException("'" + name + "' not found")
            case Some(pluginMethod) =>
                commandHandlerFactory.createHandler(pluginMethod.shellPlugin, pluginMethod.analysedMethod)
        }

    }
}
