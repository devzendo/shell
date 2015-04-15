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

import org.devzendo.shell.ShellMain.LOGGER

class Log4JLog(verbose: Boolean) extends Log {

    def isDebugEnabled = LOGGER.isDebugEnabled

    def logDebug(o: Object) {
        LOGGER.debug(o)
    }

    def logDebug(o: Object, t: Throwable) {
        LOGGER.debug(o, t)
    }

    def logDebugF(fmt: String, o: Object*) {
        LOGGER.debug(fmt.format(o))
    }

    def isInfoEnabled = LOGGER.isInfoEnabled

    def logInfo(o: Object) {
        LOGGER.info(o)
    }

    def logInfo(o: Object, t: Throwable) {
        LOGGER.info(o, t)
    }

    def logInfoF(fmt: String, o: Object*) {
        LOGGER.info(fmt.format(o))
    }

    def logWarn(o: Object) {
        LOGGER.warn(o)
    }

    def logWarn(o: Object, t: Throwable) {
        LOGGER.warn(o, t)
    }

    def logWarnF(fmt: String, o: Object*) {
        LOGGER.warn(fmt.format(o))
    }

    def logError(o: Object) {
        LOGGER.error(o)
    }

    def logError(o: Object, t: Throwable) {
        LOGGER.error(o, t)
    }

    def logErrorF(fmt: String, o: Object*) {
        LOGGER.error(fmt.format(o))
    }

    def logFatal(o: Object) {
        LOGGER.fatal(o)
    }

    def logFatal(o: Object, t: Throwable) {
        LOGGER.fatal(o, t)
    }

    def logFatalF(fmt: String, o: Object*) {
        LOGGER.fatal(fmt.format(o))
    }

    def isVerboseEnabled = verbose

    def logVerbose(o: Object) {
        if (verbose) {
            LOGGER.info(o)
        }
    }

    def logVerbose(o: Object, t: Throwable) {
        if (verbose) {
            LOGGER.info(o, t)
        }
    }

    def logVerboseF(fmt: String, o: Object*) {
        if (verbose) {
            LOGGER.info(fmt.format(o))
        }
    }
}
