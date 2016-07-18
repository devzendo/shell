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

import org.apache.log4j.Logger
import org.devzendo.shell.analyser.SemanticAnalyser

import scala.util.parsing.combinator._
import org.devzendo.shell.ast._
import org.devzendo.shell.ast.VariableReference
import org.devzendo.shell.ast.Switch
import org.devzendo.shell.ast.Command

trait ExistenceChecker {
    def exists(name: String): Boolean
}

object CommandParser {
    private val LOGGER = Logger.getLogger(classOf[CommandParser])
}

class CommandParser(commandExists: ExistenceChecker, debugParser: Boolean = false, analyser: SemanticAnalyser) {
    import CommandParser.LOGGER;

    private val LINE_SEPARATOR = System.getProperty("line.separator")

    @throws(classOf[CommandParserException])
    def parse(inputLine: String): List[Statement] = {
        def sanitizedInput = nullToEmpty(inputLine).trim()
        if (debugParser) {
            LOGGER.debug("parsing |" + sanitizedInput + "|")
        }
        if (sanitizedInput.size > 0) {
            val ccp = new StatementCombinatorParser()
            val parserOutput = ccp.parseProgram(sanitizedInput)
            parserOutput match {
                case ccp.Success(r, _) => {
                    analyser.analyse(inputLine, r)
                    val rList = r.asInstanceOf[List[Statement]] // unsure why r is not right type
                    if (debugParser) {
                        LOGGER.debug("returning " + rList.size + " AST element(s):")
                        val astDescriptions: List[String] = rList.map(ast => ast.getClass.getSimpleName + " => " + ast)
                        LOGGER.debug("  " + astDescriptions.mkString(", "))
                    }
                    return rList
                }
                case x => throw new CommandParserException(x.toString)
            }
        }
        if (debugParser) {
            LOGGER.debug("empty list")
        }
        List(new CommandPipeline())
    }

    private def nullToEmpty(input: String): String = {
        if (input == null) "" else input
    }

    private class StatementCombinatorParser extends JavaTokenParsers {
        def diag(s: String) = {
            println(s)
            true
        }

        def program: Parser[List[Statement]] = (
                statements
                // may need to introduce distinction between a program and statements
                // i.e. things only usable at the very top level
            )

        def statements: Parser[List[Statement]] = (
                rep(statement)
            )

        def statement: Parser[Statement] = (
                blockStatements | pipeline | literalAssignment | implicitEvalCommand
            )


        def blockStatements: Parser[BlockStatements] = (
                "{" ~> statements <~ "}"
            ) ^^ {
            case statements =>
                if (debugParser) LOGGER.debug("in blockStatements")
                val blockStatements = new BlockStatements()
                blockStatements.setStatements(statements)
                blockStatements
        }

        def implicitEvalCommand: Parser[CommandPipeline] = literal ~ rep(literal) ^^ {
            case firstArgument ~ remainingArgumentList =>
                if (debugParser) LOGGER.debug("in implicitEvalCommand")
                val argumentJavaList = new java.util.ArrayList[Object]
                argumentJavaList.add(firstArgument.asInstanceOf[Object])
                remainingArgumentList.foreach (x => argumentJavaList.add(x.asInstanceOf[Object]))
                val pipeline = new CommandPipeline()
                pipeline.addCommand(new Command("eval", argumentJavaList))
                pipeline
        }

        def literalAssignment: Parser[CommandPipeline] = (
                  opt(variable <~ "=") ~
                  literal ~ rep(literal) ~
                  opt(">" ~> variable) ~ opt(";")
              ) ^? ({
                  case store ~ literal ~ restOfLiterals ~ to ~ semi
                      if (store.isDefined ^ to.isDefined) => {
                      if (debugParser) LOGGER.debug("in literalAssignment")
                          val argumentJavaList = new java.util.ArrayList[Object]
                          argumentJavaList.add(literal.asInstanceOf[Object])
                          restOfLiterals.foreach (x => argumentJavaList.add(x.asInstanceOf[Object]))
                          val pipeline = new CommandPipeline()
                          val cmd: Command = new Command("eval", argumentJavaList)
                          pipeline.addCommand(cmd)
                          if (store.isDefined) {
                              pipeline.setOutputVariable(store.get)
                          }
                          if (to.isDefined) {
                              pipeline.setOutputVariable(to.get)
                          }
                          pipeline
                  }
              }, ( _ => "Use one of = and >, but not both" )
        )

        def pipeline: Parser[CommandPipeline] = (
                opt(variable <~ "=") ~
                command ~ opt("<" ~> variable)
              ~ opt("|" ~> repsep(command, "|"))
              ~ opt(">" ~> variable) ~ opt(";") // ~ opt(LINE_SEPARATOR)
              ) ^? ({
            case store ~ firstCommand ~ from ~ restCommandList ~ to ~ semi
                if (! (store.isDefined && to.isDefined)) => {
                    if (debugParser) LOGGER.debug("in pipeline")
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

        def commandVariant: Parser[Command] = (infixCommand | prefixCommand)

        def command: Parser[Command] = (
            commandVariant |
            ("(" ~> command <~ ")")
        ) ^^ ( x => {
            if (debugParser) LOGGER.debug("in command(" + x + ")")
            x
        })

        def infixCommand: Parser[Command] = argument ~ existingCommandName ~ rep(argument) ^^ {
            case firstArgument ~ name ~ remainingArgumentList =>
                if (debugParser) LOGGER.debug("in infixcommand(" + name + ", " + (firstArgument :: remainingArgumentList) + ")")
                val argumentJavaList = new java.util.ArrayList[Object]
                argumentJavaList.add(firstArgument.asInstanceOf[Object])
                remainingArgumentList.foreach (x => argumentJavaList.add(x.asInstanceOf[Object]))
                new Command(name, argumentJavaList)
        }

        def prefixCommand: Parser[Command] = existingCommandName ~ rep(argument) ^^ {
            case name ~ argumentList =>
                if (debugParser) LOGGER.debug("in prefixcommand(" + name + ", " + argumentList + ")")
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

        def identifier: Parser[String] = (ident | operatorIdentifier) ^^ ( x => {
            if (debugParser) LOGGER.debug("in identifier(" + x + ")")
            x
        })

        def existingCommandName: Parser[String] = identifier ^? ({
            case possibleCommand
                if (commandExists.exists(possibleCommand)) => {
                    if (debugParser) LOGGER.debug("in existingCommandName(" + possibleCommand + ")")
                    possibleCommand
                }
            }, ( badCommand => "Command '" + badCommand + "' is not defined")
        )

        def variable: Parser[VariableReference] = ident ^^ (x => {
            if (debugParser) LOGGER.debug("in variable(" + x + ")")
            new VariableReference(x.toString)
        })

        def wholeIntegerNumber: Parser[String] = """-?\d+(?!\.)""".r ^^ ( x => {
            if (debugParser) LOGGER.debug("in wholeIntegerNumber(" + x + ")")
            x
        })

        def argument: Parser[Any] = (
              literal
            | "(" ~> literal <~ ")"   // superfluous parenthesis but allows if (false) { ... } to be parsed as prefix
            | "(" ~> command <~ ")"
            | blockStatements
            ) ^^ ( x => {
            if (debugParser) LOGGER.debug("in argument(" + x + ")")
            x
        } )

        def literal: Parser[Any] = (
                "true" ^^^ true
              | "false" ^^^ false
              | "[-/]".r ~> ident ^^ ( x => new Switch(x.toString) )
              | wholeIntegerNumber ^^ (_.toInt)
              | floatingPointNumber ^^ (_.toDouble)
              | variable
              | stringLiteral ^^ (x => x.substring(1, x.length - 1))
              ) ^^ ( x => {
            if (debugParser) LOGGER.debug("in literal(" + x + ")")
            x
        } )
        
        def parseProgram(input: String) = {
            parseAll(program, input)
        }
    }
}