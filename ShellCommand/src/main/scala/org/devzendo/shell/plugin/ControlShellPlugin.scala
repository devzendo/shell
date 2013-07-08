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

package org.devzendo.shell.plugin

import org.devzendo.shell.interpreter.{VariableRegistry, CommandExecutionException}
import org.devzendo.shell.pipe.OutputPipe
import org.devzendo.shell.ast.BlockStatements

class ControlShellPlugin extends AbstractShellPlugin with PluginHelper {
    def getName = "Control"

    val noop = new BlockStatements()

    @CommandName(name = "if")
    @throws(classOf[CommandExecutionException])
    def conditionalBlockExecution(variableRegistry: VariableRegistry, outputPipe: OutputPipe, args: List[AnyRef]) {

        def validateBlock(block: AnyRef): BlockStatements = {
            block match {
                case (bS: BlockStatements) => bS
                case _ => throw new CommandExecutionException("Arguments to if must be a Boolean, and up to two blocks")
            }
        }

        def validateWithNoBlocks(condList: List[AnyRef]): BlockStatements = {
            validate(condList)
            noop
        }

        def validateWithThen(condList: List[AnyRef], thenBlock: AnyRef): BlockStatements = {
            val cond = validate(condList)
            val thenBlockStatements = validateBlock(thenBlock)
            if (cond) thenBlockStatements else noop
        }

        def validateWithThenAndElse(condList: List[AnyRef], thenBlock: AnyRef, elseBlock: AnyRef): BlockStatements = {
            val cond = validate(condList)
            val thenBlockStatements = validateBlock(thenBlock)
            val elseBlockStatements = validateBlock(elseBlock)
            if (cond) thenBlockStatements else elseBlockStatements
        }

        def validate(condList: List[AnyRef]): Boolean = {
            condList.apply(0) match {
                case (bCond: java.lang.Boolean) => bCond
                case _ => throw new CommandExecutionException("Argument to if must yield Boolean")
            }
        }

        /*val blockStatements =*/ args match {
                // could be argued that if with no blocks is pointless
            case List(cond) => validateWithNoBlocks(wrapArgAsList(variableRegistry)(cond))
            case List(cond, thenBlock) => validateWithThen(wrapArgAsList(variableRegistry)(cond), thenBlock)
            case List(cond, thenBlock, elseBlock) => validateWithThenAndElse(wrapArgAsList(variableRegistry)(cond), thenBlock, elseBlock)
            case _ => throw new CommandExecutionException("Arguments to if must be a Boolean, and up to two blocks")
        }

        // now execute it...
    }
}
