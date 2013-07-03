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
import org.devzendo.shell.ast._
import org.devzendo.shell.ast.VariableReference
import org.devzendo.shell.ast.Switch
import org.devzendo.shell.ast.Command

trait CommandExists {
    def commandExists(name: String): Boolean
}
class CommandParser(commandExists: CommandExists) {
    
    @throws(classOf[CommandParserException])
    def parse(inputLine: String): List[Statement] = {
        def sanitizedInput = nullToEmpty(inputLine).trim()
        if (sanitizedInput.size > 0) {
            val ccp = new StatementCombinatorParser()
            val parserOutput = ccp.parseProgram(sanitizedInput)
            parserOutput match {
                case ccp.Success(r, _) => return r
                case x => throw new CommandParserException(x.toString)
            }
        }
        List(new CommandPipeline())
    }

    private def nullToEmpty(input: String): String = {
        if (input == null) "" else input
    }

    private class StatementCombinatorParser extends JavaTokenParsers {
        def program: Parser[List[Statement]] = (
                statements
                // may need to introduce distinction between a program and statements
                // i.e. things only usable at the very top level
            )

        def statements: Parser[List[Statement]] = (
                rep(statement)
            )

        def statement: Parser[Statement] = (
                blockStatements | pipeline
            )


        def blockStatements: Parser[BlockStatements] = (
                "{" ~> statements <~ "}"
            ) ^^ {
            case statements =>
                val blockStatements = new BlockStatements()
                blockStatements.setStatements(statements)
                blockStatements
        }

        def pipeline: Parser[CommandPipeline] = (
                opt(variable <~ "=") ~
                command ~ opt("<" ~> variable)
              ~ opt("|" ~> repsep(command, "|"))
              ~ opt(">" ~> variable) ~ opt(";")
              ) ^? ({
            case store ~ firstCommand ~ from ~ restCommandList ~ to ~ semi
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

        def commandVariant: Parser[Command] = (infixCommand | prefixFunction | prefixCommand)

        def command: Parser[Command] = (
            commandVariant |
            ("(" ~> command <~ ")")
        )

        def infixCommand: Parser[Command] = argument ~ existingCommandName ~ rep(argument) ^^ {
            case firstArgument ~ name ~ remainingArgumentList =>
                val argumentJavaList = new java.util.ArrayList[Object]
                argumentJavaList.add(firstArgument.asInstanceOf[Object])
                remainingArgumentList.foreach (x => argumentJavaList.add(x.asInstanceOf[Object]))
                new Command(name, argumentJavaList)
        }

        def prefixCommand: Parser[Command] = existingCommandName ~ rep(argument) ^^ {
            case name ~ argumentList => 
                val argumentJavaList = new java.util.ArrayList[Object]
                argumentList.foreach (x => argumentJavaList.add(x.asInstanceOf[Object]))
                new Command(name, argumentJavaList)
        }

        def prefixFunction: Parser[Command] = (existingCommandName <~ "(") ~ (repsep(argument, ",") <~ ")") ^^ {
            case name ~ argumentList =>
                val argumentJavaList = new java.util.ArrayList[Object]
                argumentList.foreach (x => argumentJavaList.add(x.asInstanceOf[Object]))
                new Command(name, argumentJavaList)
        }

        def operatorIdentifier: Parser[String] =
            """[\p{Sm}\p{So}\p{Punct}&&[^()\[\]{}'"_.;`]]*""".r
        // Inspired initially from Scala's operator identifier; Odersky et al,
        // Programming in Scala, 2ed, p152.
        // I added _ to the exclusion above, but it is accepted as
        // the first character of an ident, so it is a valid identifier in
        // Shell. | is also a valid operator, but must be enclosed in (sub-
        // commands) for reasons that should be obvious :)

        def identifier: Parser[String] = (ident | operatorIdentifier)

        def existingCommandName: Parser[String] = identifier ^? ({
            case possibleCommand
                if (commandExists.commandExists(possibleCommand)) => {
                    possibleCommand
                }
            }, ( badCommand => "Command '" + badCommand + "' is not defined")
        )
        
        def variable: Parser[VariableReference] = ident ^^ (x => new VariableReference(x.toString))
        
        def wholeIntegerNumber: Parser[String] = """-?\d+(?!\.)""".r
        
        def argument: Parser[Any] = (
                "true" ^^^ true
              | "false" ^^^ false
              | "(" ~> command <~ ")"
              | "[-/]".r ~> ident ^^ ( x => new Switch(x.toString) )
              | wholeIntegerNumber ^^ (_.toInt)
              | floatingPointNumber ^^ (_.toDouble)
              | variable
              | stringLiteral ^^ (x => x.substring(1, x.length - 1))
              | blockStatements
              )
        
        def parseProgram(input: String) = {
            parseAll(program, input)
        }
    }
}