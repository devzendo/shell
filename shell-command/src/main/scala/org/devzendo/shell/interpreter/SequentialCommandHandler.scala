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

import org.apache.log4j.Logger

object SequentialCommandHandler {
    private val LOGGER = Logger.getLogger(classOf[SequentialCommandHandler])
}


class SequentialCommandHandler(listOfCommandHandlerLists: List[List[CommandHandler]]) extends CommandHandler("<block>", None, None, None, None) {


    @throws[CommandExecutionException]
    def execute() {
        SequentialCommandHandler.LOGGER.debug("starting block execution")
        listOfCommandHandlerLists.foreach( (h: List[CommandHandler]) => {
            SequentialCommandHandler.LOGGER.debug("starting block handlers execution: " + h)
            new ExecutionContainer(h).execute()
            SequentialCommandHandler.LOGGER.debug("ending block handlers execution: " + h)
        })
        SequentialCommandHandler.LOGGER.debug("ending block execution")
    }
}
