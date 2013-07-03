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

package org.devzendo.shell.plugin

import org.devzendo.shell.pipe.{OutputPipe, InputPipe}
import org.devzendo.shell.interpreter.{VariableRegistry, CommandExecutionException}
import scala.throws

class BasicOperatorsPlugin extends AbstractShellPlugin with PluginHelper {
    def getName = "Operators"

    // plus --------------------------------------------------------------------
    /*
     * Addition is defined for Integers, Doubles, Strings and Booleans.
     * Differing numerics are converted to the type that loses less.
     * If Strings are involved, addition is concatenation.
     * Numerics concatenated to Strings are first converted to Strings.
     * Boolean addition is disjunction. Booleans can only be ored with Booleans.
     */
    @CommandName(name = "+")
    @throws(classOf[CommandExecutionException])
    def plus(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[AnyRef]) {
        val validator = curriedAllowArgumentTypes("add", allArgumentTypes)(_)
        def plusElem(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aStr: String, bStr: String) => aStr + bStr
                case (aStr: String, bInt: java.lang.Integer) => plusElem(aStr, bInt.toString)
                case (aStr: String, bDbl: java.lang.Double) => plusElem(aStr, new java.lang.Double(bDbl).toString)
                case (aStr: String, bBoo: java.lang.Boolean) => throw new CommandExecutionException("Cannot add String '" + aStr + "' to Boolean '" + bBoo + "'")

                case (aInt: java.lang.Integer, bStr: String) => plusElem(aInt.toString, bStr)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt + bInt)
                case (aInt: java.lang.Integer, bDbl: java.lang.Double) => plusElem(new java.lang.Double(aInt.doubleValue()), bDbl)
                case (aInt: java.lang.Integer, bBoo: java.lang.Boolean) => throw new CommandExecutionException("Cannot add Integer '" + aInt + "' to Boolean '" + bBoo + "'")
                // what about truthiness of integers? addition of booleans means disjunction

                case (aDbl: java.lang.Double, bStr: String) => plusElem(new java.lang.Double(aDbl).toString, bStr)
                case (aDbl: java.lang.Double, bInt: java.lang.Integer) => plusElem(aDbl, new java.lang.Double(bInt.doubleValue()))
                case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Double(aDbl + bDbl)
                case (aDbl: java.lang.Double, bBoo: java.lang.Boolean) => throw new CommandExecutionException("Cannot add Double '" + aDbl + "' to Boolean '" + bBoo + "'")
                // what about truthiness of doubles? addition of booleans means disjunction

                case (aBoo: java.lang.Boolean, bStr: String) => throw new CommandExecutionException("Cannot add Boolean '" + aBoo + "' to String '" + bStr + "'")
                case (aBoo: java.lang.Boolean, bInt: java.lang.Integer) => throw new CommandExecutionException("Cannot add Boolean '" + aBoo + "' to Integer '" + bInt + "'")
                case (aBoo: java.lang.Boolean, bDbl: java.lang.Double) => throw new CommandExecutionException("Cannot add Boolean '" + aBoo + "' to Double '" + bDbl + "'")
                case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo || bBoo)
            }
        }
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, new Integer(0), plusElem, validator)
    }

    // minus -------------------------------------------------------------------
    /*
     * Subtraction is defined for Integers and Doubles.
     * Differing numerics are converted to the type that loses less.
     * Unary minus negates the inputs.
     */
    @CommandName(name = "-")
    @throws(classOf[CommandExecutionException])
    def minus(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[AnyRef]) {
        val validator = curriedAllowArgumentTypes("subtract", numericArgumentTypes)(_)
        def minusOp(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt - bInt)
                case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Double(aDbl - bDbl)
            }
        }
        val minusElem = numericCoerce(_: AnyRef, _: AnyRef)(minusOp)
        if (args.size == 1) {
            def negate(a: AnyRef): AnyRef = { minusElem(new java.lang.Integer(0), a) }
            mapArgThenPipeOut(variableRegistry, outputPipe, args(0), negate, validator)
        } else {
            reduceArgsThenPipeOut(variableRegistry, outputPipe, args, new Integer(0), minusElem, validator)
        }
    }


    // times -------------------------------------------------------------------
    /*
     * Multiplication is defined for Integers, Doubles and Strings.
     * Differing numerics are converted to the type that loses less.
     * Integer and String combinations give string replication.
     */
    @CommandName(name = "*")
    @throws(classOf[CommandExecutionException])
    def times(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("multiply", numericAndStringArgumentTypes)(_)
        def replicate(n: Integer, str: String): String = {
            if (n < 0) {
                throw new CommandExecutionException("Cannot replicate the String '" + str + "' by the negative Integer '" + n + "'")
            } else {
                str * n
            }
        }

        def timesElem(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt * bInt)
                case (aInt: java.lang.Integer, bDbl: java.lang.Double) => timesElem(new java.lang.Double(aInt.doubleValue()), bDbl)
                case (aInt: java.lang.Integer, bStr: java.lang.String) => replicate(aInt, bStr)
                case (aStr: String, bInt: java.lang.Integer) => replicate(bInt, aStr)

                case (aDbl: java.lang.Double, bInt: java.lang.Integer) => timesElem(aDbl, new java.lang.Double(bInt.doubleValue()))
                case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Double(aDbl * bDbl)
                case (aDbl: java.lang.Double, bStr: java.lang.String) => throw new CommandExecutionException("Cannot replicate the String '" + bStr + "' by the Double '" + aDbl + "'")
                case (aStr: String, bDbl: java.lang.Double) => throw new CommandExecutionException("Cannot replicate the String '" + aStr + "' by the Double '" + bDbl + "'")
            }
        }
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, new Integer(1), timesElem, validator)
    }


    // divide ------------------------------------------------------------------
    /*
     * Division is defined for Integers and Doubles.
     * Differing numerics are converted to the type that loses less.
     */
    @CommandName(name = "/")
    @throws(classOf[CommandExecutionException])
    def divide(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("divide", numericArgumentTypes)(_)
        def divideOp(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt / bInt)
                case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Double(aDbl / bDbl)
            }
        }
        val divideElem = numericCoerce(_: AnyRef, _: AnyRef)(divideOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, new Integer(1), divideElem, validator)
    }

    // logical not -------------------------------------------------------------
    /*
     * Negation is only defined for Booleans, and is a unary operation.
     */
    @CommandName(name = "!")
    @CommandAlias(alias = "¬")
    @throws(classOf[CommandExecutionException])
    def logicalNot(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("negate", booleanArgumentTypes)(_)
        if (args.size == 1) {
            def negate(a: AnyRef): AnyRef = a match {
                case b: java.lang.Boolean => new java.lang.Boolean(!b)
            }
            mapArgThenPipeOut(variableRegistry, outputPipe, args(0), negate, validator)
        } else {
            throw new CommandExecutionException("Boolean negation is a unary operation")
        }
    }

    // modulus -----------------------------------------------------------------
    /*
     * Modulus is defined for Integers
     */
    @CommandName(name = "%")
    @throws(classOf[CommandExecutionException])
    def mod(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("take the modulus of", integerArgumentTypes)(_)
        def modElem(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt % bInt)
            }
        }
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, new Integer(1), modElem, validator)
    }

    // bitwise exclusive or ----------------------------------------------------
    /*
     * Xor is defined for Integers and Booleans.
     */
    @CommandName(name = "^")
    @throws(classOf[CommandExecutionException])
    def bitwiseXor(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("bitwise xor", integerBooleanArgumentTypes)(_)
        def xorOp(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo ^ bBoo)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt ^ bInt)
            }
        }
        val xorElem = bitwiseCoerce(_: AnyRef, _: AnyRef)(xorOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, new Integer(0), xorElem, validator)
    }

    // bitwise or --------------------------------------------------------------
    /*
     * Or is defined for Integers and Booleans.
     */
    @CommandName(name = "|")
    @throws(classOf[CommandExecutionException])
    def bitwiseOr(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("bitwise or", integerBooleanArgumentTypes)(_)
        def orOp(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo | bBoo)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt | bInt)
            }
        }
        val orElem = bitwiseCoerce(_: AnyRef, _: AnyRef)(orOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, new Integer(0), orElem, validator)
    }

    // bitwise and -------------------------------------------------------------
    /*
     * And is defined for Integers and Booleans.
     */
    @CommandName(name = "&")
    @throws(classOf[CommandExecutionException])
    def bitwiseAnd(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("bitwise and", integerBooleanArgumentTypes)(_)
        def andOp(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo & bBoo)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new Integer(aInt & bInt)
            }
        }
        val andElem = bitwiseCoerce(_: AnyRef, _: AnyRef)(andOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, new Integer(0), andElem, validator)
    }

    // bitwise complement ------------------------------------------------------
    /*
     * Complement is defined for Integers and Booleans.
     */
    @CommandName(name = "~")
    @throws(classOf[CommandExecutionException])
    def bitwiseComplement(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("complement", integerBooleanArgumentTypes)(_)
        if (args.size == 1) {
            def complement(a: AnyRef): AnyRef = a match {
                case b: java.lang.Boolean => new java.lang.Boolean(!b)
                case i: java.lang.Integer => new java.lang.Integer(~i)
            }
            mapArgThenPipeOut(variableRegistry, outputPipe, args(0), complement, validator)
        } else {
            throw new CommandExecutionException("Bitwise complement is a unary operation")
        }
    }

    // logical and -------------------------------------------------------------
    /*
     * Logical and is defined for Booleans.
     */
    @CommandName(name = "&&")
    @throws(classOf[CommandExecutionException])
    def logicalAnd(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("logically and", booleanArgumentTypes)(_)
        def andElem(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo & bBoo)
            }
        }
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, java.lang.Boolean.FALSE, andElem, validator)
    }

    // logical or --------------------------------------------------------------
    /*
     * Logical or is defined for Booleans.
     */
    @CommandName(name = "||")
    @throws(classOf[CommandExecutionException])
    def logicalOr(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("logically or", booleanArgumentTypes)(_)
        def orElem(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo | bBoo)
            }
        }
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, java.lang.Boolean.FALSE, orElem, validator)
    }

    // logical xor -------------------------------------------------------------
    /*
     * Logical xor is defined for Booleans.
     */
    @CommandName(name = "^^")
    @throws(classOf[CommandExecutionException])
    def logicalXor(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("logically xor", booleanArgumentTypes)(_)
        def xorElem(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aBoo: java.lang.Boolean, bBoo: java.lang.Boolean) => new java.lang.Boolean(aBoo ^ bBoo)
            }
        }
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, java.lang.Boolean.FALSE, xorElem, validator)
    }

    // ordering relations ------------------------------------------------------
    /*
     * Ordering relations are defined for Integers, Doubles and Strings (via
     * lexicographic comparison).
     */
    @CommandName(name = "<") // hmmm parser?
    @throws(classOf[CommandExecutionException])
    def lessThan(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("order", numericAndStringArgumentTypes)(_)
        def ltOp(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aStr: String, bStr: String) => new java.lang.Boolean(aStr.compareTo(bStr) < 0)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new java.lang.Boolean(aInt < bInt)
                case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Boolean(aDbl < bDbl)
            }
        }
        val ltElem = alphaNumericCoerce(_: AnyRef, _: AnyRef)(ltOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, java.lang.Boolean.FALSE, ltElem, validator)
    }

    @CommandName(name = ">") // hmmm parser?
    @throws(classOf[CommandExecutionException])
    def greaterThan(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("order", numericAndStringArgumentTypes)(_)
        def gtOp(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aStr: String, bStr: String) => new java.lang.Boolean(aStr.compareTo(bStr) > 0)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new java.lang.Boolean(aInt > bInt)
                case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Boolean(aDbl > bDbl)
            }
        }
        val gtElem = alphaNumericCoerce(_: AnyRef, _: AnyRef)(gtOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, java.lang.Boolean.FALSE, gtElem, validator)
    }

    @CommandName(name = "<=") // hmmm parser?
    @CommandAlias(alias = "≤")
    @throws(classOf[CommandExecutionException])
    def lessThanOrEqual(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("order", numericAndStringArgumentTypes)(_)
        def ltEqOp(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aStr: String, bStr: String) => new java.lang.Boolean(aStr.compareTo(bStr) <= 0)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new java.lang.Boolean(aInt <= bInt)
                case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Boolean(aDbl <= bDbl)
            }
        }
        val ltEqElem = alphaNumericCoerce(_: AnyRef, _: AnyRef)(ltEqOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, java.lang.Boolean.FALSE, ltEqElem, validator)
    }

    @CommandName(name = ">=") // hmmm parser?
    @CommandAlias(alias = "≥")
    @throws(classOf[CommandExecutionException])
    def greaterThanOrEqual(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("order", numericAndStringArgumentTypes)(_)
        def gtEqOp(a: AnyRef, b: AnyRef): AnyRef = {
            (a, b) match {
                case (aStr: String, bStr: String) => new java.lang.Boolean(aStr.compareTo(bStr) >= 0)
                case (aInt: java.lang.Integer, bInt: java.lang.Integer) => new java.lang.Boolean(aInt >= bInt)
                case (aDbl: java.lang.Double, bDbl: java.lang.Double) => new java.lang.Boolean(aDbl >= bDbl)
            }
        }
        val gtEqElem = alphaNumericCoerce(_: AnyRef, _: AnyRef)(gtEqOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, java.lang.Boolean.FALSE, gtEqElem, validator)
    }

    // value equality / inequality ---------------------------------------------

    @CommandName(name = "!=")
    @CommandAlias(alias = "<>")
//    @CommandAlias(alias = "≠")
    @throws(classOf[CommandExecutionException])
    def notEqual(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("compare", allArgumentTypes)(_)
        def notEqOp(a: AnyRef, b: AnyRef): AnyRef = {
            new java.lang.Boolean(a != b)
        }
        val notEqElem = alphaNumericCoerceBooleanPassthrough(_: AnyRef, _: AnyRef)(notEqOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, java.lang.Boolean.FALSE, notEqElem, validator)
    }

    @CommandName(name = "==")
    @throws(classOf[CommandExecutionException])
    def equal(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {
        val validator = curriedAllowArgumentTypes("compare", allArgumentTypes)(_)
        def eqOp(a: AnyRef, b: AnyRef): AnyRef = {
            new java.lang.Boolean(a == b)
        }
        val eqElem = alphaNumericCoerceBooleanPassthrough(_: AnyRef, _: AnyRef)(eqOp)
        reduceArgsThenPipeOut(variableRegistry, outputPipe, args, java.lang.Boolean.FALSE, eqElem, validator)
    }

    // object equality / inequality --------------------------------------------

    @CommandName(name = "ne")
    @throws(classOf[CommandExecutionException])
    def notEqualReference(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {

    }

    @CommandName(name = "eq")
    @throws(classOf[CommandExecutionException])
    def equalReference(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: List[Object]) {

    }

    // bit shifts --------------------------------------------------------------

    @CommandName(name = ">>") // hmmm parser?
    def shiftRight(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = ">>>") // hmmm parser?
    def shiftRightUnsigned(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }

    @CommandName(name = "<<") // hmmm parser?
    def shiftLeft(variableRegistry: VariableRegistry, inputPipe: InputPipe, outputPipe: OutputPipe, args: java.util.List[Object]) {

    }
}

