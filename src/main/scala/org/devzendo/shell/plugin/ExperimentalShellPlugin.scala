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
import org.apache.log4j.Logger
import org.devzendo.shell.pipe.{InputPipe, OutputPipe}
import org.devzendo.shell.ShellMain.LOGGER
import scala.collection.JavaConversions._
import scala.io.Source
import scala.Option
import scala.util.matching.Regex


object ExperimentalShellPlugin {
    private val LOGGER = Logger.getLogger(classOf[ExperimentalShellPlugin])
}
class ExperimentalShellPlugin extends AbstractShellPlugin with PluginHelper {
    def getName() = {
        "Experimental"
    }

    // count -------------------------------------------------------------------
    def count(outputPipe: OutputPipe, args: java.util.List[Object]) = {
        val first = Integer.parseInt(args.get(0).toString)
        val last = Integer.parseInt(args.get(1).toString)
        first to last foreach(outputPipe.push)
    }
    
    // cat ---------------------------------------------------------------------
    def cat(outputPipe: OutputPipe, args: java.util.List[Object]) = {
        filterString(args).foreach(catFile(_, outputPipe))
    }
    
    private def catFile(filename: Object, outputPipe: OutputPipe) = {
        if (new File(filename.toString).exists) {
            Source.fromFile(filename.toString).getLines.foreach(outputPipe.push)
        } else {
            LOGGER.warn("cat: File '" + filename + "' does not exist");
        }
    }
    
    // filterRegex -------------------------------------------------------------
    def filterRegex(inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) = {
        val patternSeq = filterValidPatterns(args)
        val filterOutput = new OutputIfMatching(patternSeq, outputPipe)
        streamMap(inputPipe.next(), (a: Object) => filterOutput.filterOutput(a))
    }
    
    private def filterValidPatterns(possRegexs: java.util.List[Object]): Seq[Pattern] = {
        filterString(possRegexs).map(validRegex).flatten
    }
    
    private def validRegex(possRegex: String): Option[Pattern] = {
        try {
            Some(Pattern.compile(possRegex))
        } catch {
            case ex: PatternSyntaxException => {
                LOGGER.warn("filter: Regex syntax error '" + possRegex + "' : " + ex.getMessage())
                None
            }
        }
    }
    
    private class OutputIfMatching(patterns: Seq[Pattern], outputPipe: OutputPipe) {
        def filterOutput(o: Object) = {
            val anyFound = patterns.map {
                pattern => pattern.matcher(o.toString).find
            }
            if (anyFound.exists(_ == true)) {
                outputPipe.push(o.toString)
            }
        }
    }
}