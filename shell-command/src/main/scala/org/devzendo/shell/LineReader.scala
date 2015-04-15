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

import jline.console.ConsoleReader
import jline.console.history.FileHistory
import java.io.File
import jline.console.completer.{Completer, CompletionHandler}
import java.util

/*
 * Much gratitude to sbt's authors, as this JLine integration was taken from
 * theirs and simplified a little.
 */
trait LineReader {
    // TODO remove this when the calling code is
    // translated from Java to Scala (passing Option.None from Java is a PITA)
    def readLine(prompt: String): Option[String] = readLine(prompt, None)

    def readLine(prompt: String, mask: Option[Char] = None): Option[String]
}

final class JLineLineReader(historyPath: File, completionHandler: CompletionHandler) extends LineReader {
    private[this] val reader: ConsoleReader = {
        val cr = JLine.createReader(historyPath)
        // add a dummy completer, to force use of the CompletionHandler
        cr.addCompleter(new Completer() {
            def complete(buffer: String, cursor: Int, candidates: util.List[CharSequence]) = {
                candidates.asInstanceOf[java.util.List[String]] add "ignore"
                0
            }
        })
        cr.setCompletionHandler(completionHandler)
        cr
    }

    private[this] val handleCONT = JLine.HandleCONT

    def readLine(prompt: String, mask: Option[Char] = None) = JLine.withJLine {
        unsynchronizedReadLine(prompt, mask)
    }

    private[this] def unsynchronizedReadLine(prompt: String, mask: Option[Char]) =
        readLineWithHistory(prompt, mask) match {
            case null => None
            case x => Some(x.trim)
        }

    private[this] def readLineWithHistory(prompt: String, mask: Option[Char]): String =
        reader.getHistory match {
            case fh: FileHistory =>
                try {
                    readLineDirect(prompt, mask)
                } finally {
                    fh.flush()
                }
            case _ => readLineDirect(prompt, mask)
        }

    private[this] def readLineDirect(prompt: String, mask: Option[Char]): String =
        if (handleCONT)
            Signals.withHandler(() => resume(), signal = Signals.CONT)( () => readLineDirectRaw(prompt, mask) )
        else
            readLineDirectRaw(prompt, mask)

    private[this] def readLineDirectRaw(prompt: String, mask: Option[Char]): String = {
        val newprompt = handleMultilinePrompt(prompt)
        mask match {
            case Some(m) => reader.readLine(newprompt, m)
            case None => reader.readLine(newprompt)
        }
    }

    private[this] def handleMultilinePrompt(prompt: String): String = {
        val lines = """\r?\n""".r.split(prompt)
        lines.size match {
            case 0 | 1 => prompt
            case _ => reader.print(lines.init.mkString("\n") + "\n"); lines.last
        }
    }

    private[this] def resume() {
        jline.TerminalFactory.reset()
        JLine.terminal.setEchoEnabled(false)
        reader.drawLine()
        reader.flush()
    }
}

object JLine {
    // When calling this, ensure that enableEcho has been or will be called.
    // TerminalFactory.get will initialize the terminal to disable echo.
    def terminal = jline.TerminalFactory.get
    private def withTerminal[T](f: jline.Terminal => T): T =
        synchronized {
            val t = terminal
            t.synchronized { f(t) }
        }

    /**
     * For accessing the JLine Terminal object.
     * This ensures synchronized access as well as re-enabling echo after getting the Terminal.
     */
    def usingTerminal[T](f: jline.Terminal => T): T =
        withTerminal { t =>
            t.setEchoEnabled(true)
            f(t)
        }

    def createReader(historyPath: File): ConsoleReader =
        usingTerminal { t =>
            val cr = new ConsoleReader
            cr.setBellEnabled(false)
            val h = new FileHistory(historyPath)
            h.setMaxSize(MaxHistorySize)
            cr.setHistory(h)
            cr
        }

    def withJLine[T](action: => T): T =
        withTerminal { t =>
            t.setEchoEnabled(false)
            try {
                action
            }
            finally {
                t.setEchoEnabled(true)
            }
        }

    val MaxHistorySize = 500
    val HandleCONT = Signals.supported(Signals.CONT)
}

object Signals {
    val CONT = "CONT"
    val INT = "INT"

    def withHandler[T](handler: () => Unit, signal: String = INT)(action: () => T): T = {
        val result =
            try {
                val signals = new Signals0
                signals.withHandler(signal, handler, action)
            }
            catch {
                case e: LinkageError => Right(action())
            }

        result match {
            case Left(e) => throw e
            case Right(v) => v
        }
    }

    def supported(signal: String): Boolean =
        try {
            val signals = new Signals0
            signals.supported(signal)
        }
        catch {
            case e: LinkageError => false
        }
}

// Must only be referenced using a
//   try { } catch { case e: LinkageError => ... }
// block to
private final class Signals0 {
    def supported(signal: String): Boolean = {
        import sun.misc.Signal
        try {
            new Signal(signal); true
        }
        catch {
            case e: IllegalArgumentException => false
        }
    }

    // returns a LinkageError in `action` as Left(t) in order to avoid it being
    // incorrectly swallowed as missing Signal/SignalHandler
    def withHandler[T](signal: String, handler: () => Unit, action: () => T): Either[Throwable, T] = {
        import sun.misc.{Signal,SignalHandler}
        val intSignal = new Signal(signal)
        val newHandler = new SignalHandler {
            def handle(sig: Signal) {
                handler()
            }
        }

        val oldHandler = Signal.handle(intSignal, newHandler)

        try
            Right(action())
        catch {
            case e: LinkageError => Left(e)
        }
        finally Signal.handle(intSignal, oldHandler)
    }
}
