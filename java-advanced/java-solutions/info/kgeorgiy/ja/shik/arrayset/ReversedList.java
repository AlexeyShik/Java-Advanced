package info.kgeorgiy.ja.shik.arrayset;

import java.util.*;

class ReversedList<E> extends AbstractList<E> implements RandomAccess {
    private final List<E> elements;

    public static <T> List<T> generate(final List<T> elements) {
        if (elements instanceof ReversedList) {
            return ((ReversedList<T>) elements).elements;
        } else {
            return new ReversedList<>(elements);
        }
    }

    ReversedList(final List<E> elements) {
        this.elements = elements;
    }

    @Override
    public E get(final int index) {
        return elements.get(size() - index - 1);
    }

    @Override
    public int size() {
        return elements.size();
    }
}
