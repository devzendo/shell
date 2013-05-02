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

package org.devzendo.shell.parser

import scala.util.parsing.combinator._
import org.devzendo.shell.ast.{Switch, Command, CommandPipeline, VariableReference}

class CommandParser {
    
    @throws(classOf[CommandParserException])
    def parse(inputLine: String): CommandPipeline = {
        def sanitizedInput = nullToEmpty(inputLine).trim()
        if (sanitizedInput.size > 0) {
            val ccp = new CommandCombinatorParser()
            val parserOutput = ccp.parsePipeline(sanitizedInput)
            parserOutput match {
                case ccp.Success(r, _) => return r
                case x => throw new CommandParserException(x.toString)
            }
        }
        new CommandPipeline()
    }
    
    private def nullToEmpty(input: String): String = {
        if (input == null) "" else input
    }
    
    private class CommandCombinatorParser extends JavaTokenParsers {
        def pipeline: Parser[CommandPipeline] = (
                opt(variable <~ "=") ~
                command ~ opt("<" ~> variable)
              ~ opt("|" ~> repsep(command, "|")) 
              ~ opt(">" ~> variable)
              ) ^? ({
            case store ~ firstCommand ~ from ~ restCommandList ~ to
                if (! (store.isDefined && to.isDefined)) => {
                    val pipeline = new CommandPipeline()
                    pipeline.addCommand(firstCommand)
                    if (store.isDefined) {
                        pipeline.setOutputVariable(store.get)
                    }
                    if (from.isDefined) {
                        pipeline.setInputVariable(from.get)
                    }
                    if (to.isDefined) {
                        pipeline.setOutputVariable(to.get)
                    }
                    if (restCommandList.isDefined) {
                        pipeline.addCommands(restCommandList.get)
                    }
                    pipeline
                }
            }, ( _ => "Use one of = and >, but not both" )
            )

        def command: Parser[Command] = ident ~ rep(argument) ^^ {
            case name ~ argumentList => 
                val argumentJavaList = new java.util.ArrayList[Object]
                argumentList.foreach (x => argumentJavaList.add(x.asInstanceOf[Object]))
                new Command(name, argumentJavaList)
        }
        
        def variable: Parser[VariableReference] = ident ^^ (x => new VariableReference(x.toString))
        
        def wholeIntegerNumber: Parser[String] = """-?\d+(?!\.)""".r
        
        def argument: Parser[Any] = (
                "true" ^^^ true
              | "false" ^^^ false
              | "[-/]".r ~> ident ^^ ( x => new Switch(x.toString) )
              | wholeIntegerNumber ^^ (_.toInt)
              | floatingPointNumber ^^ (_.toDouble)
              | variable 
              | stringLiteral ^^ (x => x.substring(1, x.length - 1))
              )
        
        def parsePipeline(input: String) = {
            parseAll(pipeline, input)
        }
    }
}