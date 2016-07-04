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

import org.devzendo.shell.pipe.InputPipe
import org.devzendo.shell.ShellMain.LOGGER
import org.devzendo.shell.interpreter.Log

class LoggingShellPlugin extends AbstractShellPlugin with PluginHelper {
    def getName() = {
        "Logging"
    }
    
    // Log each InputPipe object at various levels...
    def logDebug(inputPipe: InputPipe) {
        streamForeach(inputPipe.next(), (a: Object) => LOGGER.debug(a))
    }
    def logInfo(inputPipe: InputPipe) {
        streamForeach(inputPipe.next(), (a: Object) => LOGGER.info(a))
    }
    def logWarn(inputPipe: InputPipe) {
        streamForeach(inputPipe.next(), (a: Object) => LOGGER.warn(a))
    }
    def logError(inputPipe: InputPipe) {
        streamForeach(inputPipe.next(), (a: Object) => LOGGER.error(a))
    }
    def logFatal(inputPipe: InputPipe) {
        streamForeach(inputPipe.next(), (a: Object) => LOGGER.fatal(a))
    }

    def logVerbose(inputPipe: InputPipe, log: Log) {
        streamForeach(inputPipe.next(), (a: Object) => log.logVerbose(a))
    }
}