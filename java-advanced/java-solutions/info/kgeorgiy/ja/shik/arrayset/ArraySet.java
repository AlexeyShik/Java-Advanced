package info.kgeorgiy.ja.shik.arrayset;
import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final List<E> elements;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        elements = Collections.emptyList();
        comparator = null;
    }

    public ArraySet(Collection<E> collection) {
        elements = new ArrayList<>(new TreeSet<>(collection));
        comparator = null;
    }

    public ArraySet(Collection<E> collection, Comparator<? super E> comparator) {
        TreeSet<E> set = new TreeSet<>(comparator);
        set.addAll(collection);
        elements = new ArrayList<>(set);
        this.comparator = comparator;
    }

    private ArraySet(List<E> elements, Comparator<? super E> comparator) {
        this.elements = elements;
        this.comparator = comparator;
    }

    private int search(E o) {
        return Collections.binarySearch(elements, o, this::compare);
    }

    private boolean checkIndex(int i) {
        return i >= 0 && i < size();
    }

    private int getIndex(E e, int shiftIfFound, int shiftIfNotFound) {
        int i = search(e);
        return (i >= 0 ? i + shiftIfFound : -i - 1 + shiftIfNotFound);
    }

    private E getElement(int i) {
        return checkIndex(i) ? elements.get(i) : null;
    }

    private E getElement(E e, int shiftIfFound, int shiftIfNotFound) {
        return getElement(getIndex(e, shiftIfFound, shiftIfNotFound));
    }

    @Override
    public E lower(E e) {
        return getElement(e, -1, -1);
    }

    @Override
    public E floor(E e) {
        return getElement(e, 0, -1);
    }

    @Override
    public E ceiling(E e) {
        return getElement(e, 0, 0);
    }

    @Override
    public E higher(E e) {
        return getElement(e, 1, 0);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator() {
        return new UnmodifiableIterator<>(elements.iterator());
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(ReversedList.generate(elements), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    private ArraySet<E> emptySet() {
        return new ArraySet<>(List.of(), comparator);
    }

    private ArraySet<E> makeSubSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int left = getIndex(fromElement, fromInclusive ? 0 : 1, 0);
        int right = getIndex(toElement, toInclusive ? 0 : -1, -1);
        if (isEmpty() || left > right || left <= -1 || right >= size()) {
            return emptySet();
        }
        return new ArraySet<>(elements.subList(left, right + 1), comparator);
    }

    @SuppressWarnings("unchecked")
    private int compare(E e1, E e2) {
        return comparator == null ? ((Comparable<? super E>) e1).compareTo(e2) : comparator.compare(e1, e2);
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return makeSubSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return isEmpty() ? emptySet() : makeSubSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return isEmpty() ? emptySet() : makeSubSet(fromElement, inclusive, last(), true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    private void requireNonEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E first() {
        requireNonEmpty();
        return elements.get(0);
    }

    @Override
    public E last() {
        requireNonEmpty();
        return elements.get(size() - 1);
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return search((E) o) >= 0;
    }

    private static class UnmodifiableIterator<E> implements Iterator<E> {
        private final Iterator<E> iterator;

        public UnmodifiableIterator(Iterator<E> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public E next() {
            return iterator.next();
        }
    }
}
