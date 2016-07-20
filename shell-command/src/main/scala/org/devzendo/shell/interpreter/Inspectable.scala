package org.devzendo.shell.interpreter

trait Inspectable {
    def inspect(output: String => Unit): Unit
}
