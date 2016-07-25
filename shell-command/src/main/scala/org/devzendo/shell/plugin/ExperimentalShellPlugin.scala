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
 
package org.devzendo.shell.plugin

import java.io.File
import java.util.regex.{Pattern, PatternSyntaxException}

import org.devzendo.shell.pipe.{InputPipe, OutputPipe}
import org.devzendo.shell.ShellMain.LOGGER

import scala.collection.JavaConversions._
import scala.util.control.Breaks._
import scala.io.Source
import scala.Option
import org.devzendo.shell.ast.VariableReference
import org.devzendo.shell.interpreter.{CommandExecutionException, Inspectable, VariableRegistry}

class ExperimentalShellPlugin extends AbstractShellPlugin with PluginHelper {
    def getName = {
        "Experimental"
    }

    // envargs -----------------------------------------------------------------
    def envargs(outputPipe: OutputPipe, args: java.util.List[Object]) {
        val envArgs = executionEnvironment().argList()
        envArgs.foreach(outputPipe.push)
    }

    // count -------------------------------------------------------------------
    def count(outputPipe: OutputPipe, args: java.util.List[Object]) {
        val first = Integer.parseInt(args.get(0).toString)
        val last = Integer.parseInt(args.get(1).toString)

        first to last foreach( (x: Int) => outputPipe.push(x.asInstanceOf[Integer]) )
    }

    // cat ---------------------------------------------------------------------
    def cat(outputPipe: OutputPipe, args: java.util.List[Object]) {
        filterString(args).foreach(catFile(_, outputPipe))
    }
    
    private def catFile(filename: Object, outputPipe: OutputPipe) {
        if (new File(filename.toString).exists) {
            Source.fromFile(filename.toString).getLines().foreach(outputPipe.push)
        } else {
            LOGGER.warn("cat: File '" + filename + "' does not exist")
        }
    }
    
    // filterRegex -------------------------------------------------------------
    def filterRegex(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {
        val patternSeq = filterValidPatterns(args)
        LOGGER.debug("patternSeq is " + patternSeq)
        def filterOutput(o: Object) = {
            patternSeq.find { (pattern: Pattern) =>  
                val objString = o.toString
                LOGGER.debug("matching '" + objString + "'")
                val matcher = pattern.matcher(objString)
                if (matcher.matches) {
                    LOGGER.debug("it matches")
                    outputPipe.push(new MatchContext(objString, (1 to matcher.groupCount) map matcher.group))
                    true
                } else {
                    LOGGER.debug("does not match")
                    false
                }
            }
        }

        streamForeach(inputPipe.next(), (a: Object) => filterOutput(a))
    }
    
    private class MatchContext(val inputString: String, val captureGroups: Seq[String]) extends Inspectable {
        override def toString: String = inputString

        override def inspect(output: (String) => Unit): Unit = {
            output.apply("MatchContext(" + inputString + "), " + numberPlusPluralDescription(captureGroups.size, "capture group"))
            for (index <- captureGroups.indices) {
                output.apply("  #" + (index + 1) + ": " + captureGroups.get(index))
            }
        }
    }


    // 1 house, 2 houses
    // 1 cat, 3 cats
    // 1 capture group, 4 capture groups
    // 1 sheep, 2 sheeps (oh well...)
    def numberPlusPluralDescription(size: Int, desc: String): String = {
        size.toString + " " + desc + (if (size != 1) "s" else "")
    }

    private def filterValidPatterns(possRegexs: java.util.List[Object]): Seq[Pattern] = {
        filterString(possRegexs).map(validPattern).flatten
    }
    
    private def validPattern(possRegex: String): Option[Pattern] = {
        try {
            Some(Pattern.compile(possRegex))
        } catch {
            case ex: PatternSyntaxException => {
                LOGGER.warn("Regex syntax error: " + ex.getMessage)
                None
            }
        }
    }

    // cut ---------------------------------------------------------------------
    // cut takes MatchContexts (capture groups) and cuts out specific ones, 
    // pushing ArrayBuffer of cut capture groups. Indices start at 1.
    def cut(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {
       val captureGroups = filterInt(args)
       streamForeach(inputPipe.next(), (a: Object) => a match {
           case mc: MatchContext =>
               outputPipe.push(for (group <- captureGroups) yield mc.captureGroups(group - 1))
           case s: String => // in case we got here straight from cat
               LOGGER.info("Got string '" + s + "' - cut doesn't know what to do with Strings")
       })
    }


    // matches -----------------------------------------------------------------
    // matches takes MatchContexts (capture groups) on input and outputs the matching text.
    def matches(inputPipe: InputPipe, outputPipe: OutputPipe) {
        streamForeach(inputPipe.next(), (a: Object) => a match {
            case mc: MatchContext =>
                outputPipe.push(mc.inputString)
            case s: String => // in case we got here straight from cat
                LOGGER.info("Got string '" + s + "' - match doesn't know what to do with Strings")
        })
    }

    // echo --------------------------------------------------------------------
    def echo(variableRegistry: VariableRegistry, outputPipe: OutputPipe, args: java.util.List[Object]) {
        args.foreach((arg: AnyRef) => {
            arg match {
                case varRef: VariableReference => {
                    LOGGER.debug("looking up variable reference " + varRef + " in variable registry " + variableRegistry)
                    if (variableRegistry.exists(varRef)) {
                        outputPipe.push(variableRegistry.getVariable(varRef).get)
                    } else {
                        LOGGER.warn("No such variable '" + varRef.variableName + "'")
                    }
                }
                case x: AnyRef =>
                    outputPipe.push(x.toString)
            }
        })
    }

    // inspect -----------------------------------------------------------------
    def inspect(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {
        def inspectIt: (AnyRef) => Unit = {
            (arg: AnyRef) => {
                arg match {
                    case varRef: VariableReference => {
                        LOGGER.debug("looking up variable reference " + varRef + " in variable registry " + variableRegistry)
                        if (variableRegistry.exists(varRef)) {
                            outputPipe.push(variableRegistry.getVariable(varRef).get)
                        } else {
                            LOGGER.warn("No such variable '" + varRef.variableName + "'")
                        }
                    }
                    case insp: Inspectable =>
                        insp.inspect(outputPipe.push(_))
                    case x: AnyRef =>
                        outputPipe.push(x.toString)
                }
            }
        }

        LOGGER.debug("inspecting args...")
        args.foreach(inspectIt)
        LOGGER.debug("inspecting input pipe...")
        streamForeach(inputPipe.next(), inspectIt)
    }

    // head --------------------------------------------------------------------
    // head N takes N entries from the input pipe and pushes them onto the output pipe, then stops.
    def head(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {
        val numLinesSeq = filterInt(args)
        if (numLinesSeq.size == 1) {
            val numLines: Int = numLinesSeq.get(0)
            LOGGER.debug("head passing " + numLines + " object(s)")
            breakable {
                for (n <- 0 until numLines) {
                    LOGGER.debug("getting head object " + (n+1) + "/" + numLines)
                    val obj = inputPipe.next()
                    obj match {
                        case Some(x) => {
                            LOGGER.debug("pushing head object '" + x + "'")
                            outputPipe.push(x)
                            LOGGER.debug("pushed head object")
                        }
                        case None =>
                            LOGGER.debug("input pipe empty")
                            break()
                    }
                }
            }
            LOGGER.debug("head finished")
        } else {
            throw new CommandExecutionException("head takes a single integer argument")
        }
    }

}