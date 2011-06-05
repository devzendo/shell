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
import scala.Option

class LoggingShellPlugin extends AbstractShellPlugin {
    def getName() = {
        "Logging"
    }
    
    def processStreamOfFunctionCallsReturningOptionUntilNone(
            producer: => Option[Object], processor: (Object) => Unit) {
        // There has to be a more idiomatic way of doing this...
        var obj: Option[Object] = None
        do {
            obj = producer
            obj.map(processor(_))
        } while (obj.isDefined)
    }
    
    // InputPipe is defined in Java as:
    // Option<Object> next();
    // So: log every object coming down the pipe until it's empty (None).
    // Later, I want to do other arbitrary things to the pipe contents until
    // exhausted.
    def logInfo(inputPipe: InputPipe) = {
        processStreamOfFunctionCallsReturningOptionUntilNone(
            inputPipe.next(), 
            (a: Object) => LOGGER.info(a))
    }
}