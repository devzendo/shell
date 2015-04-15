package org.devzendo.shell;

import scala.collection.immutable.List;
import scala.collection.immutable.Nil$;

public abstract class ScalaListHelper {
    private ScalaListHelper() {}

    public static List<Object> createObjectList(final Object ... elems) {
        return createList(elems);
    }

    public static <T> List<T> createList(final T... elems) {
        List list = Nil$.MODULE$;
        for (T elem : elems) {
            list = list.$colon$colon(elem);
        }
        return list.reverse();
    }
}