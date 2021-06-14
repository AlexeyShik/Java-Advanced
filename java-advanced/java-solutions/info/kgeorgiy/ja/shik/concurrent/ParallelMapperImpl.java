package info.kgeorgiy.ja.shik.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final LinkedBlockingQueue<Runnable> tasks;
    private boolean terminated;

    /**
     * Constructor from number of {@code Thread}'s to use
     *
     * @param nThreads - number of {@code Thread}'s to use
     */
    public ParallelMapperImpl(int nThreads) {
        if (nThreads < 1) {
            throw new IllegalArgumentException("Number of threads should be >= 1");
        }
        threads = new ArrayList<>();
        tasks = new LinkedBlockingQueue<>();
        terminated = false;
        final Runnable runnable = () -> {
            try {
                while (!Thread.interrupted()) {
                    tasks.poll().run();
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        while (nThreads > 0) {
            final Thread thread = new Thread(runnable);
            thread.start();
            threads.add(thread);
            --nThreads;
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        if (terminated) {
            throw new IllegalStateException("Mapper is already terminated");
        }
        final ResultList<R> result = new ResultList<>(list.size());
        for (int i = 0; i < list.size(); ++i) {
            final int finalI = i;
            tasks.add(() -> {
                try {
                    result.set(finalI, function.apply(list.get(finalI)));
                } catch (final RuntimeException e) {
                    result.setException(e);
                }
            });
        }
        return result.getResults();
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        if (!terminated) {
            terminated = true;
            threads.forEach(Thread::interrupt);
            for (int i = 0; i < threads.size(); ++i) {
                try {
                    threads.get(i).join();
                } catch (final InterruptedException e) {
                    --i;
                }
            }
        }
    }

    /**
     * Thread-safe {@code Queue} for queueing tasks
     *
     * @param <T> type of tasks in {@code Queue}
     */
    private static class LinkedBlockingQueue<T> {
        private final Queue<T> queue;

        private LinkedBlockingQueue() {
            queue = new LinkedList<>();
        }

        public synchronized void add(final T value) {
            queue.add(value);
            notify();
        }

        public synchronized T poll() throws InterruptedException {
            while (queue.isEmpty()) {
                wait();
            }
            return queue.poll();
        }
    }

    /**
     * Thread-safe {@code List} for collecting results of processed tasks
     *
     * @param <R> type of result of tasks
     */
    private static class ResultList<R> {
        private final List<R> list;
        private RuntimeException exception;
        int remain;

        private ResultList(final int size) {
            list = new ArrayList<>(Collections.nCopies(size, null));
            remain = size;
            exception = null;
        }

        public synchronized void set(final int index, final R value) {
            list.set(index, value);
            --remain;
            if (remain == 0) {
                notify();
            }
        }

        public synchronized List<R> getResults() throws InterruptedException {
            while (remain != 0) {
                wait();
            }
            if (exception == null) {
                return list;
            }
            throw exception;
        }

        public synchronized void setException(final RuntimeException e) {
            if (exception == null) {
                exception = new RuntimeException("Runtime exception occurs while processing tasks", e);
            } else {
                exception.addSuppressed(e);
            }
        }
    }
}
