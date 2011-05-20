/**
 * Copyright (C) 2008-2010 Matt Gumbley, DevZendo.org <http://devzendo.org>
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
 
package org.devzendo.shell

import java.util.ArrayList
import scala.collection.JavaConversions._
import scala.util.parsing.combinator._

case class VariableReference(val variableName: String)

class CommandParser {
    
    @throws(classOf[CommandParserException])
    def parse(inputLine: String): CommandPipeline = {
        def sanitizedInput = nullToEmpty(inputLine).trim()
        if (sanitizedInput.size > 0) {
            val ccp = new CommandCombinatorParser()
            val parserOutput = ccp.parsePipeline(sanitizedInput)
            parserOutput match {
                case ccp.Success(r, _) => return r
                case x => throw new CommandParserException(x.toString())
            }
        }
        return new CommandPipeline()
    }
    
    private def nullToEmpty(input: String): String = {
        if (input == null) "" else input
    }
    
    private class CommandCombinatorParser extends JavaTokenParsers {
        def pipeline: Parser[CommandPipeline] = (
                opt(">" ~> variable) // not happy with this syntax, but it avoids ambiguity
              ~ repsep(command, "|") 
              ~ opt(">" ~> variable)
              ) ^^ {
            case from ~ commandList ~ to =>
                val pipeline = new CommandPipeline()
                if (!from.isEmpty) {
                    pipeline.setInputVariable(from.get)
                }
                if (!to.isEmpty) {
                    pipeline.setOutputVariable(to.get)
                }
                pipeline.addCommands(commandList)
                pipeline
        }
        
        def command: Parser[Command] = ident ~ rep(argument) ^^ {
            case name ~ argumentList => 
                val argumentJavaList = new java.util.ArrayList[Object]
                argumentList.foreach (x => argumentJavaList.add(x.asInstanceOf[Object]))
                new Command(name, argumentJavaList)
        }
        
        def variable: Parser[VariableReference] = ident ^^ (x => new VariableReference(x.toString))
        
        def wholeIntegerNumber: Parser[String] = """-?\d+(?!\.)""".r
        
        def argument: Parser[Any] = (
                "true" ^^ (x => true)
              | "false" ^^ (x => false)
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