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

package org.devzendo.shell

import scala.collection.JavaConverters._
import org.devzendo.shell.ast.VariableReference

final class CommandPipeline {
    var commands = scala.collection.mutable.ArrayBuffer[Command]()

    @scala.reflect.BeanProperty
    var inputVariable: VariableReference = null

    @scala.reflect.BeanProperty
    var outputVariable: VariableReference = null

    def getCommands: java.util.List[Command] = {
        commands.asJava
    }

    // TODO convert this to Scala List
    def addCommands(commandsToAdd: java.util.List[Command]) {
        (0 until commandsToAdd.size()).foreach( commands += commandsToAdd.get(_) )
    }

    def addCommand(commandToAdd: Command) {
        commands += commandToAdd
    }

    def isEmpty: Boolean = {
        commands.isEmpty
    }
}
