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

import scala.Predef.String
import collection.JavaConverters._
import scala.collection.mutable
import org.devzendo.shell.plugin.{ShellPluginException, ShellPlugin}

class DefaultPluginRegistry(val propertiesResourcePath: String, val commandRegistry: CommandRegistry,
    val variableRegistry: VariableRegistry,
    val argList: List[String]) extends PluginRegistry {

    private val pluginMethodScanner = new PluginMethodScanner()
    private val plugins = scala.collection.mutable.Set[ShellPlugin]()

    @throws[ShellPluginException]
    def loadAndRegisterPluginMethods(staticPlugins: List[ShellPlugin]) {
        val env: ExecutionEnvironment = new DefaultExecutionEnvironment(argList, commandRegistry, variableRegistry, this)
        val allPlugins = loadAllPlugins(staticPlugins.toBuffer)
        for (shellPlugin <- allPlugins) {
            plugins += shellPlugin
            shellPlugin.initialise(env)
            val nameMethodMap = pluginMethodScanner.scanPluginMethods(shellPlugin)
            for (entry <- nameMethodMap) {
                try {
                    commandRegistry.registerCommand(entry._1, shellPlugin, entry._2)
                } catch {
                    case e: DuplicateCommandException => {
                        throw new ShellPluginException(e.getMessage)
                    }
                }
            }
        }
    }

    @throws[ShellPluginException]
    private[this] def loadAllPlugins(staticPlugins: mutable.Buffer[ShellPlugin]): List[ShellPlugin] = {
        val pluginsFromClasspath = new PluginLoader().loadPluginsFromClasspath(propertiesResourcePath)
        List[ShellPlugin]() ++ staticPlugins ++ pluginsFromClasspath
    }

    def getPlugins: java.util.Set[ShellPlugin] = {
        plugins.asJava
    }
}

