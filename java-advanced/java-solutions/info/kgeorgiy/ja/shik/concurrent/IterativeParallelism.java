package info.kgeorgiy.ja.shik.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper mapper;

    /**
     * No-arg constructor for {@code IterativeParallelism}
     */
    public IterativeParallelism() {
        mapper = null;
    }

    /**
     * Constructs {@code IterativeParallelism} from given {@link ParallelMapper}
     *
     * @param mapper {@code ParallelMapper} for constructing
     */
    public IterativeParallelism(final ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T> List<List<T>> makeGroups(final int nGroups, final List<T> list) {
        if (nGroups < 1) {
            throw new IllegalArgumentException("Number of threads should be >= 1");
        }
        final int groupSize = list.size() / nGroups;
        int remain = list.size() % nGroups;
        int left = 0;
        final List<List<T>> groups = new ArrayList<>();
        for (int i = 0; i < nGroups; ++i) {
            final int right = left + groupSize + (i < remain ? 1 : 0);
            groups.add(list.subList(left, Math.min(right, list.size())));
            left = right;
        }
        return groups;
    }

    private <T> void joinThreads(final List<List<T>> groups, final List<Thread> threads) throws InterruptedException {
        InterruptedException exception = null;
        for (int i = 0; i < groups.size(); ++i) {
            try {
                threads.get(i).join();
            } catch (final InterruptedException e) {
                if (exception == null) {
                    exception = new InterruptedException("Interrupted while waiting for workers");
                }
                exception.addSuppressed(e);
                --i;
                threads.get(i).interrupt();
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private <T, R> R processGroups(int nGroups, final List<T> list,
                                   final Function<Stream<T>, R> groupFunction,
                                   final Function<Stream<R>, R> reduceFunction) throws InterruptedException {
        nGroups = Math.min(nGroups, list.size());
        final List<List<T>> groups = makeGroups(nGroups, list);

        if (mapper == null) {
            final List<Thread> threads = new ArrayList<>();
            final List<R> groupResults = new ArrayList<>(Collections.nCopies(nGroups, null));
            for (int i = 0; i < groups.size(); ++i) {
                final int finalI = i;
                final Thread thread = new Thread(() ->
                        groupResults.set(finalI, groupFunction.apply(groups.get(finalI).stream())));
                threads.add(thread);
                thread.start();
            }
            joinThreads(groups, threads);
            return reduceFunction.apply(groupResults.stream());
        } else {
            final List<R> results = mapper.map(x -> groupFunction.apply(x.stream()), groups);
            final List<List<R>> resGroups = makeGroups(Math.min(2, groups.size()), results);
            final List<R> merged = mapper.map((List<R> l) -> reduceFunction.apply(l.stream()), resGroups);
            return reduceFunction.apply(merged.stream());
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T> T orElseThrow(final Optional<T> optionalT) {
        return optionalT.orElseThrow(NoSuchElementException::new);
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number or concurrent threads.
     * @param list       values to get maximum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(final int threads, final List<? extends T> list,
                         final Comparator<? super T> comparator) throws InterruptedException {
        return processGroups(threads, list,
                stream -> orElseThrow(stream.max(comparator)),
                stream -> orElseThrow(stream.reduce(BinaryOperator.maxBy(comparator))));
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number or concurrent threads.
     * @param list       values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(final int threads, final List<? extends T> list,
                         final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, list, comparator.reversed());
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param list      values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(final int threads, final List<? extends T> list,
                           final Predicate<? super T> predicate) throws InterruptedException {
        return processGroups(threads, list,
                stream -> stream.allMatch(predicate),
                stream -> stream.reduce(true, Boolean::logicalAnd));
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number or concurrent threads.
     * @param list      values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(final int threads, final List<? extends T> list,
                           final Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, list, predicate.negate());
    }

    private String joining(final Stream<String> stream) {
        return stream.collect(Collectors.joining());
    }

    /**
     * Join values to string.
     *
     * @param threads number of concurrent threads.
     * @param list    values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(final int threads, final List<?> list) throws InterruptedException {
        return processGroups(threads, list, stream -> joining(stream.map(Object::toString)), this::joining);
    }

    private <T> List<T> toList(final Stream<? extends T> stream) {
        return stream.collect(Collectors.toList());
    }

    private <T> List<T> flatMapToList(final Stream<List<T>> stream) {
        return toList(stream.flatMap(Collection::stream));
    }

    private <T, U> List<U> flatProcessGroups(final int threads, final List<T> list, final Function<Stream<T>,
            Stream<? extends U>> function) throws InterruptedException {
        return processGroups(threads, list, stream -> toList(function.apply(stream)), this::flatMapToList);
    }

    /**
     * Filters values by predicate.
     *
     * @param threads   number of concurrent threads.
     * @param list      values to filter.
     * @param predicate filter predicate.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> list,
                              final Predicate<? super T> predicate) throws InterruptedException {
        return flatProcessGroups(threads, list, stream -> stream.filter(predicate));
    }

    /**
     * Maps values.
     *
     * @param threads  number of concurrent threads.
     * @param list     values to filter.
     * @param function mapper function.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> list,
                              final Function<? super T, ? extends U> function) throws InterruptedException {
        return flatProcessGroups(threads, list, stream -> stream.map(function));
    }

    private <T, R> Function<Stream<T>, R> generateReduceFunction(final Function<T, R> function, final Monoid<R> monoid) {
        return stream -> stream.map(function).reduce(monoid.getOperator()).orElse(monoid.getIdentity());
    }

    /**
     * Reduces values using monoid.
     *
     * @param threads number of concurrent threads.
     * @param list    values to reduce.
     * @param monoid  monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> T reduce(final int threads, final List<T> list, final Monoid<T> monoid) throws InterruptedException {
        final Function<Stream<T>, T> reduceFunction = generateReduceFunction(Function.identity(), monoid);
        return processGroups(threads, list, reduceFunction, reduceFunction);
    }

    /**
     * Maps and reduces values using monoid.
     *
     * @param threads  number of concurrent threads.
     * @param list     values to reduce.
     * @param function mapping function.
     * @param monoid   monoid to use.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, R> R mapReduce(final int threads, final List<T> list, final Function<T, R> function,
                              final Monoid<R> monoid) throws InterruptedException {
        return processGroups(threads, list,
                generateReduceFunction(function, monoid),
                generateReduceFunction(Function.identity(), monoid));
    }
}
