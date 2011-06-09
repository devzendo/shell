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
 
package org.devzendo.shell.plugin

import org.apache.log4j.Level
import org.devzendo.shell.pipe.{InputPipe, OutputPipe}
import org.devzendo.shell.ShellMain.LOGGER
import scala.Option

class LoggingShellPlugin extends AbstractShellPlugin with PluginHelper {
    def getName() = {
        "Logging"
    }
    
    private def logInputPipeAtLevel(inputPipe: InputPipe, level: Level) = {
        streamMap(inputPipe.next(), (a: Object) => LOGGER.log(level, a))
    }

    // Log each InputPipe object at various levels...
    def logDebug(inputPipe: InputPipe) = {
        logInputPipeAtLevel(inputPipe, Level.DEBUG)
    }
    def logInfo(inputPipe: InputPipe) = {
        logInputPipeAtLevel(inputPipe, Level.INFO)
    }
    def logWarn(inputPipe: InputPipe) = {
        logInputPipeAtLevel(inputPipe, Level.WARN)
    }
    def logError(inputPipe: InputPipe) = {
        logInputPipeAtLevel(inputPipe, Level.ERROR)
    }
    def logFatal(inputPipe: InputPipe) = {
        logInputPipeAtLevel(inputPipe, Level.FATAL)
    }
    
    def count(outputPipe: OutputPipe, args: java.util.List[Object]) = {
        val first = Integer.parseInt(args.get(0).toString)
        val last = Integer.parseInt(args.get(1).toString)
        first to last foreach(outputPipe.push(_))
    }
}