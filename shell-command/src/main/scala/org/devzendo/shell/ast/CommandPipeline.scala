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

package org.devzendo.shell.ast

final class CommandPipeline extends Statement {
    var commands = scala.collection.mutable.ArrayBuffer[Command]()

    @scala.reflect.BeanProperty
    var inputVariable: VariableReference = null

    @scala.reflect.BeanProperty
    var outputVariable: VariableReference = null

    def getCommands: List[Command] = {
        commands.toList
    }

    def addCommands(commandsToAdd: List[Command]) {
        commands ++= commandsToAdd
    }

    def addCommand(commandToAdd: Command) {
        commands += commandToAdd
    }

    def isEmpty: Boolean = {
        commands.isEmpty
    }

    override def toString(): String = {
        "<" + inputVariable + " [" +
        commands.mkString(" ") + "] >" + inputVariable
    }
}