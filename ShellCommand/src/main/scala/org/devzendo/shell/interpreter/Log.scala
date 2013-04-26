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

trait Log {
    def isDebugEnabled: Boolean
    def logDebug(o: Object)
    def logDebug(o: Object, t: Throwable)
    def logDebugF(fmt: String, o: Object*)

    def isInfoEnabled: Boolean
    def logInfo(o: Object)
    def logInfo(o: Object, t: Throwable)
    def logInfoF(fmt: String, o: Object*)

    def logWarn(o: Object)
    def logWarn(o: Object, t: Throwable)
    def logWarnF(fmt: String, o: Object*)

    def logError(o: Object)
    def logError(o: Object, t: Throwable)
    def logErrorF(fmt: String, o: Object*)

    def logFatal(o: Object)
    def logFatal(o: Object, t: Throwable)
    def logFatalF(fmt: String, o: Object*)

    def isVerboseEnabled: Boolean
    def logVerbose(o: Object)
    def logVerbose(o: Object, t: Throwable)
    def logVerboseF(fmt: String, o: Object*)
}
