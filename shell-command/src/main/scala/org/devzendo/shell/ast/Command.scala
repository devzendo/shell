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

package org.devzendo.shell.ast

import scala.collection.JavaConversions._

case class Command(
    @scala.beans.BeanProperty
    name: String,

    @scala.beans.BeanProperty
    args: java.util.List[AnyRef]) {

    override def toString(): String = {
        "Command " + name + " (" + (args.map( (x: AnyRef) => x.getClass().getSimpleName + "@" + x).mkString(", ")) + ")"
    }
}
