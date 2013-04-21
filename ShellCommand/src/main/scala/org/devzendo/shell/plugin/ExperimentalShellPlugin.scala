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
import scala.io.Source
import scala.Option

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
        def filterOutput(o: Object) = {
            patternSeq.find { (pattern: Pattern) =>  
                val objString = o.toString
                val matcher = pattern.matcher(objString)
                if (matcher.matches) {
                    outputPipe.push(new MatchContext(objString, (1 to matcher.groupCount) map matcher.group))
                    true
                } else {
                    false
                }
            }
        }

        streamForeach(inputPipe.next(), (a: Object) => filterOutput(a))
    }
    
    private class MatchContext(val inputString: String, val captureGroups: Seq[String]) {
        override def toString: String = inputString
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
}