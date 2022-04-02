package com.observability.printservice.utils;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * @since 4.37
 * @author Jaroslav Tulach
 */
public class Utils {
    public static <E> Iterable<E> iterable(final Enumeration<E> enumeration) {
        if (enumeration == null) {
            throw new NullPointerException();
        }
        return new Iterable<E>() {
            public Iterator<E> iterator() {
                return new Iterator<E>() {
                    public boolean hasNext() {
                        return enumeration.hasMoreElements();
                    }
                    public E next() {
                        return enumeration.nextElement();
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
