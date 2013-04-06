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

trait PluginHelper {
    def streamForeach(producer: => Option[Object], processor: (Object) => Unit) {
        Stream.continually(producer).takeWhile(_.isDefined).flatten.foreach(processor)
    }

    def streamMap(producer: => Option[Object], processor: (Object) => Object): Stream[Object] = {
        Stream.continually(producer).takeWhile(_.isDefined).flatten.map(processor)
    }

    def filterString(objects: Seq[Object]):Seq[String] = objects.filter(_.isInstanceOf[String]).asInstanceOf[Seq[String]] 

    def filterInt(objects: Seq[Object]):Seq[Integer] = objects.filter(_.isInstanceOf[Integer]).asInstanceOf[Seq[Integer]] 

}