package info.kgeorgiy.ja.shik.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;
    private final ExecutorService downloadService;
    private final ExecutorService extractorService;
    private final int perHost;
    private Map<String, Host> hosts;

    /**
     * Constructs web-crawler.
     *
     * @param downloader  {@link Downloader} instance for downloading pages using {@link Downloader#download(String)}.
     * @param downloaders maximum number of pages, that could be downloaded at the same time.
     * @param extractors  maximum number of pages, from that links could be extracted at the same time.
     * @param perHost     maximum number of pages for a fixed host, that could be downloaded at the same time.
     */
    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        downloadService = Executors.newFixedThreadPool(downloaders);
        extractorService = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    /**
     * Downloads web site up to specified depth.
     *
     * @param url   start <a href="http://tools.ietf.org/html/rfc3986">URL</a>.
     * @param depth download depth.
     * @return download result.
     */
    @Override
    public Result download(final String url, final int depth) {
        return download(url, depth, List.of(), false);
    }

    /**
     * Closes this web-crawler, relinquishing any allocated resources.
     */
    @Override
    public void close() {
        shutdownAndAwaitTermination(downloadService);
        shutdownAndAwaitTermination(extractorService);
    }

    private void shutdownAndAwaitTermination(final ExecutorService service) {
        service.shutdown();
        try {
            if (!service.awaitTermination(1, TimeUnit.MINUTES)) {
                service.shutdownNow();
                if (!service.awaitTermination(1, TimeUnit.MINUTES))
                    System.err.println("Service did not terminate");
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void process(final int depth, final Set<String> downloaded, final Map<String, IOException> errors,
                         Set<String> prevLevelURLs, final List<String> permittedHosts,
                         final boolean needCheck) {
        final Phaser phaser = new Phaser(1);
        while (phaser.getPhase() < depth) {
            final Set<String> newLevelURLs = ConcurrentHashMap.newKeySet();
            final Set<String> finalPrevLevelURLs = prevLevelURLs;
            prevLevelURLs.forEach(url -> processURL(depth, url, phaser,
                    downloaded, errors, finalPrevLevelURLs, newLevelURLs, permittedHosts, needCheck));
            phaser.arriveAndAwaitAdvance();
            prevLevelURLs = newLevelURLs;
        }
    }

    private void processURL(final int depth, final String url, final Phaser phaser, final Set<String> downloaded,
                            final Map<String, IOException> errors, final Set<String> prevLevelURLs,
                            final Set<String> newLevelURLs, final List<String> permittedHosts, final boolean needCheck) {
        try {
            final String hostName = URLUtils.getHost(url);
            if (needCheck && !permittedHosts.contains(hostName)) {
                return;
            }
            hosts.putIfAbsent(hostName, new Host());
            final Host host = hosts.get(hostName);
            phaser.register();
            host.addTask(() -> {
                try {
                    final Document document = downloader.download(url);
                    downloaded.add(url);
                    if (phaser.getPhase() + 1 < depth) {
                        phaser.register();
                        extractorService.submit(() -> {
                            try {
                                document.extractLinks()
                                        .stream()
                                        .filter(x -> !(downloaded.contains(x) || errors.containsKey(x)
                                                || prevLevelURLs.contains(x)))
                                        .forEach(newLevelURLs::add);
                            } catch (IOException ignored) {
                                //  no operations
                            } finally {
                                phaser.arriveAndDeregister();
                            }
                        });
                    }
                } catch (IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arriveAndDeregister();
                }
            });
        } catch (MalformedURLException e) {
            errors.put(url, e);
        }
    }

    /**
     * Downloads web site up to specified depth.
     *
     * @param url start <a href="http://tools.ietf.org/html/rfc3986">URL</a>.
     * @param depth download depth.
     * @param permittedHosts domains to follow, pages on another domains should be ignored.
     * @return download result.
     */
    @Override
    public Result download(final String url, final int depth, final List<String> permittedHosts) {
        return download(url, depth, permittedHosts, true);
    }

    private Result download(final String url, final int depth, final List<String> permittedHosts, final boolean needCheck) {
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final Set<String> levelURLs = ConcurrentHashMap.newKeySet();
        hosts = new ConcurrentHashMap<>();
        levelURLs.add(url);
        process(depth, downloaded, errors, levelURLs, permittedHosts, needCheck);
        return new Result(new ArrayList<>(downloaded), errors);
    }



    private class Host {
        private final Queue<Runnable> waiting;
        private final Semaphore semaphore;

        private Host() {
            waiting = new ConcurrentLinkedQueue<>();
            semaphore = new Semaphore(perHost);
        }

        private void addTask(final Runnable task) {
            final Runnable r = () -> {
                task.run();
                if (waiting.isEmpty()) {
                    semaphore.release();
                } else {
                    downloadService.submit(waiting.poll());
                }
            };
            if (semaphore.tryAcquire()) {
                downloadService.submit(r);
            } else {
                waiting.add(r);
            }
        }
    }


    /**
     * Invokes web-crawler with given arguments.
     *
     * @param args command line arguments in format: url [depth [downloads [extractors [perHost]]]].
     *             <ul>
     *                         <li>
     *                              url - start <a href="http://tools.ietf.org/html/rfc3986">URL</a>.
     *                         </li>
     *                         <li>
     *                              depth - download depth.
     *                         </li>
     *                         <li>
     *                              downloaders - maximum number of pages, that could be downloaded at the same time.
     *                         </li>
     *                         <li>
     *                              extractors - maximum number of pages, from that links could be extracted at the same time.
     *                         </li>
     *                         <li>
     *                              perHost - maximum number of pages for a fixed host, that could be downloaded at the same time.
     *                         </li>
     *             </ul>
     */
    public static void main(final String[] args) {
        if (args == null || args.length == 0 || args.length > 5) {
            System.err.println("Usage: WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments shouldn't be null");
            return;
        }
        final int depth = getOrDefault(args, 1, 1);
        final int downloaders = getOrDefault(args, 2, 1);
        final int extractors = getOrDefault(args, 3, 1);
        final int perHost = getOrDefault(args, 4, 4);
        try (final WebCrawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
            Result result = crawler.download(args[0], depth);
            System.out.println("Successfully downloaded pages:");
            result.getDownloaded().forEach(System.out::println);
            System.out.println("Pages downloaded with errors:");
            result.getErrors().forEach((key, value) -> System.out.printf("Page: %s, exception: %s%n", key, value));
        } catch (IOException e) {
            System.err.println("Cannot create new CachingDownloader");
        }
    }

    private static int getOrDefault(final String[] args, final int index, final int defaultValue) {
        try {
            return index < args.length ? Integer.parseInt(args[index]) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
