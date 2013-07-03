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

import org.devzendo.shell.interpreter.{Variable, VariableRegistry, CommandExecutionException}
import org.devzendo.shell.ast.VariableReference
import scala.annotation.tailrec
import org.devzendo.shell.pipe.OutputPipe

trait PluginHelper {
    def streamForeach(producer: => Option[Object], processor: (Object) => Unit) {
        Stream.continually(producer).takeWhile(_.isDefined).flatten.foreach(processor)
    }

    def streamMap(producer: => Option[Object], processor: (Object) => Object): Stream[Object] = {
        Stream.continually(producer).takeWhile(_.isDefined).flatten.map(processor)
    }

    def filterString(objects: Seq[Object]):Seq[String] = objects.filter(_.isInstanceOf[String]).asInstanceOf[Seq[String]] 

    def filterInt(objects: Seq[Object]):Seq[Integer] = objects.filter(_.isInstanceOf[Integer]).asInstanceOf[Seq[Integer]] 

    def filterBoolean(objects: Seq[Object]):Seq[Boolean] = objects.filter(_.isInstanceOf[Boolean]).asInstanceOf[Seq[Boolean]]

    @throws(classOf[CommandExecutionException])
    def onlyAllowArgumentTypes(commandNameAsVerb: String, args: List[AnyRef], allowedClasses: Seq[Class[_]]) {
        val argsAndTheirClasses = args.map( (arg: AnyRef) => {
            val argClass = arg match {
                case null => classOf[Null].asInstanceOf[Class[_]]
                case x: AnyRef => x.getClass.asInstanceOf[Class[_]]
            }
            (arg, argClass)
        })
        val allowedClassesSet = allowedClasses.toSet
        val disallowedArgsAndTheirClasses = argsAndTheirClasses.filterNot( (aatc: (AnyRef, Class[_])) => {
            allowedClassesSet.contains(aatc._2)
        })
        if (disallowedArgsAndTheirClasses.size > 0)
        {
            // (Switch("foo"), classOf[Switch]) => "Switch 'foo'"
            val argDescriptions = disallowedArgsAndTheirClasses.map((aatc: (AnyRef, Class[_])) =>
                aatc._2.getSimpleName + " '" + aatc._1 + "'"
            )
            throw new CommandExecutionException("Cannot " + commandNameAsVerb + " the " + argDescriptions.mkString(", "))
        }
    }

    // Convert an arg to a list, expanding variable references.
    def wrapArgAsList(variableRegistry: VariableRegistry)(arg: AnyRef): List[AnyRef] = arg match {
        case v: Variable =>
            v.asList()
        case vr: VariableReference =>
            variableRegistry.getVariable(vr).asList()
        case null => // unsure...
            List[AnyRef]()
        case x: AnyRef =>
            List(x)
    }

    // Convert all args to lists, expanding variable references.
    def wrapAsList(variableRegistry: VariableRegistry)(args: List[AnyRef]): List[List[AnyRef]] = {
        // LOGGER.debug("Wrapping args as lists: " + dump(args))
        val out = args map wrapArgAsList(variableRegistry)
        // LOGGER.debug("Wrapped args as lists: " + out)
        out
    }

    def dump(args: List[AnyRef]): String = {
        args map {
            (a: AnyRef) => {
                ("<" + a.getClass + "> " + a)
            }
        } mkString(",")
    }

    // Pad all lists out to the same length with some identity.
    def padLists(args: List[List[AnyRef]], fill: AnyRef) = {
        val maxLen = (0 /: args) ((curLen: Int, list: List[AnyRef]) =>  Math.max(curLen, list.size))
        // LOGGER.debug("Padding args with fill(" + fill + ") to maxlen(" + maxLen + ") args: " + args)
        val out = args.map( (argList: List[AnyRef]) => {
            argList.padTo(maxLen, fill)
        } )
        // LOGGER.debug("Padded args: " + out)
        out
    }

    // Reduce two lists by applying an operation to elements of them, pairwise, producing one output list.
    // e.g. applying + to ((1,2), (3,4)) => (4,6)
    def reducePair(first: List[AnyRef], second: List[AnyRef], op: ((AnyRef, AnyRef) => AnyRef)): List[AnyRef] = {
        (first zip second).map( (t: (AnyRef, AnyRef)) => { op(t._1, t._2) } )
    }

    // Repeatedly reduce a list with an operation until it is empty or a single list.
    @tailrec
    final def reduce(args: List[List[AnyRef]], op: ((AnyRef, AnyRef) => AnyRef)): List[AnyRef] = {
        args.size match {
            case 0 => List.empty
            case 1 => args.apply(0)
            case _ => {
                val first = args.apply(0)
                val second = args.apply(1)
                val rest = args.drop(2)
                val reduced = reducePair(first, second, op) :: rest
                reduce(reduced, op)
            }
        }
    }

    // Reduce expanded arguments to a single list transformed by an operation and identity, and pipe the results out.
    def reduceArgsThenPipeOut(
             variableRegistry: VariableRegistry,
             outputPipe: OutputPipe,
             args: List[AnyRef],
             identity: AnyRef,
             op: ((AnyRef, AnyRef) => AnyRef),
             validate: (List[AnyRef]) => Unit) {
        // LOGGER.debug("Wrapping args: " + args)
        val argsAsLists = wrapAsList(variableRegistry)(args)
        // LOGGER.debug("Validating wrapped args: " + argsAsLists)
        argsAsLists foreach validate
        // LOGGER.debug("Padding validated wrapped args")
        val argsAsPaddedLists = padLists(argsAsLists, identity)
        // LOGGER.debug("Reducing args: " + argsAsPaddedLists)
        val reduced = reduce(argsAsPaddedLists, op)
        // LOGGER.debug("Reduced args: " + dump(reduced))

        reduced.foreach( outputPipe.push(_) )
    }

    // Map expanded argument to a single list transformed by an operation, and pipe the results out.
    def mapArgThenPipeOut(
             variableRegistry: VariableRegistry,
             outputPipe: OutputPipe,
             arg: AnyRef,
             op: ((AnyRef) => AnyRef),
             validate: (List[AnyRef]) => Unit) {
        // LOGGER.debug("Wrapping arg: " + arg)
        val argList = wrapArgAsList(variableRegistry)(arg)
        // LOGGER.debug("Validating wrapped arg: " + argList)
        validate(argList)
        // LOGGER.debug("Mapping arg")
        val mapped = argList map op
        // LOGGER.debug("Mapped arg: " + mapped)
        mapped.foreach( outputPipe.push(_) )
    }

    // Coerce dissimilar String/Numeric arguments "upwards":
    // Double, Integer -> String
    // Integer -> Double
    // .. then perform some other operation on the pair that are now the same type.
    def alphaNumericCoerce(a: AnyRef, b: AnyRef)(op: ((AnyRef, AnyRef) => AnyRef)): AnyRef = {
        (a, b) match {
            case (aStr: String, bStr: String) => op(a, b)
            case (aStr: String, bInt: java.lang.Integer) => op(aStr, bInt.toString)
            case (aStr: String, bDbl: java.lang.Double) => op(aStr, new java.lang.Double(bDbl).toString)

            case (aInt: java.lang.Integer, bStr: String) => op(aInt.toString, bStr)
            case (aInt: java.lang.Integer, bInt: java.lang.Integer) => op(a, b)
            case (aInt: java.lang.Integer, bDbl: java.lang.Double) => op(new java.lang.Double(aInt.doubleValue()), bDbl)

            case (aDbl: java.lang.Double, bStr: String) => op(new java.lang.Double(aDbl).toString, bStr)
            case (aDbl: java.lang.Double, bInt: java.lang.Integer) => op(aDbl, new java.lang.Double(bInt.doubleValue()))
            case (aDbl: java.lang.Double, bDbl: java.lang.Double) => op(a, b)
        }
    }

    // Coerce dissimilar String/Numeric arguments "upwards", preserve Booleans,
    // but compositions of this must be prepared to handle Booleans with anything.
    // Double, Integer -> String
    // Integer -> Double
    // .. then perform some other operation on the pair that are now the same type.
    def alphaNumericCoerceBooleanPassthrough(a: AnyRef, b: AnyRef)(op: ((AnyRef, AnyRef) => AnyRef)): AnyRef = {
        val parentCoerce = alphaNumericCoerce(_: AnyRef, _: AnyRef)(op)
        (a, b) match {
            case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => op(a, b)
            case (aBoo: java.lang.Boolean, _: AnyRef) => op(a, b)
            case (_: AnyRef, bBoo: java.lang.Boolean) => op(a, b)

            case (_: AnyRef, _: AnyRef) => parentCoerce(a, b)
        }
    }

    // Coerce dissimilar Numeric arguments "upwards":
    // Integer -> Double
    // .. then perform some other operation on the pair that are now the same type.
    def numericCoerce(a: AnyRef, b: AnyRef)(op: ((AnyRef, AnyRef) => AnyRef)): AnyRef = {
        (a, b) match {
            case (aInt: java.lang.Integer, bInt: java.lang.Integer) => op(a, b)
            case (aInt: java.lang.Integer, bDbl: java.lang.Double) => op(new java.lang.Double(aInt.doubleValue()), bDbl)

            case (aDbl: java.lang.Double, bInt: java.lang.Integer) => op(aDbl, new java.lang.Double(bInt.doubleValue()))
            case (aDbl: java.lang.Double, bDbl: java.lang.Double) => op(a, b)
        }
    }

    private def boolean2Integer(b: java.lang.Boolean): Integer = {
        if (b) new Integer(1) else new Integer(0)
    }

    // Coerce dissimilar Integer/Bitwise arguments "upwards":
    // Boolean -> Integer
    // .. then perform some other operation on the pair that are now the same type.
    def bitwiseCoerce(a: AnyRef, b: AnyRef)(op: ((AnyRef, AnyRef) => AnyRef)): AnyRef = {
        (a, b) match {
            case (aInt: java.lang.Integer, bBoo: java.lang.Boolean) => op(aInt, boolean2Integer(bBoo))
            case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => op(a, b)
            case (aInt: java.lang.Integer, bInt: java.lang.Integer) => op(a, b)
            case (aBoo: java.lang.Boolean, bInt: java.lang.Integer) => op(boolean2Integer(aBoo), bInt)
        }
    }

    val allArgumentTypes = Seq(
        classOf[String], classOf[java.lang.Integer], classOf[java.lang.Double], classOf[java.lang.Boolean],
        classOf[Variable], classOf[VariableReference]
    )

    val integerArgumentTypes = Seq(
        classOf[java.lang.Integer],
        classOf[Variable], classOf[VariableReference]
    )

    val integerBooleanArgumentTypes = Seq(
        classOf[java.lang.Integer], classOf[java.lang.Boolean],
        classOf[Variable], classOf[VariableReference]
    )

    val numericArgumentTypes = Seq(
        classOf[java.lang.Integer], classOf[java.lang.Double],
        classOf[Variable], classOf[VariableReference]
    )

    val booleanArgumentTypes = Seq(
        classOf[java.lang.Boolean], classOf[Variable], classOf[VariableReference]
    )

    val numericAndStringArgumentTypes = Seq(
        classOf[java.lang.Integer], classOf[java.lang.Double],
        classOf[Variable], classOf[VariableReference], classOf[String]
    )

    def curriedAllowArgumentTypes(verb: String, allowed: Seq[Class[_]])(args: List[AnyRef])
    {
        onlyAllowArgumentTypes(verb, args, allowed)
    }
}