package info.kgeorgiy.ja.shik.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelloUtils {
    // :NOTE: Переиспользование - fixed
    static final Pattern pattern = Pattern.compile("\\D*(\\d+)\\D*(\\d+)\\D*");
    static final int TIMEOUT = 50;
    static final int MAX_PORT_VALUE = 0xFFFF;

    static boolean validatePortAndThreads(final int port, final int threads) {
        if (port < 0 || port > HelloUtils.MAX_PORT_VALUE) {
            HelloUtils.logError("Port out of range: expected from 0 to 65535, found " + port);
            return false;
        }
        if (threads < 1) {
            HelloUtils.logError("Number of threads out of range: expected >= 1, found " + threads);
            return false;
        }
        return true;
    }

    static SocketAddress validateAndGetAddress(final String host, final int port, final int threads, final int requests) {
        if (!validatePortAndThreads(port, threads)) {
            return null;
        }
        if (requests < 0) {
            HelloUtils.logError("Number of requests out of range: expected >= 0, found " + requests);
        }
        try {
            return new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (final UnknownHostException e) {
            HelloUtils.logError("Unknown host found: ", e);
            return null;
        }
    }

    static void logInfo(final String sendData, final String receiveData) {
        System.out.printf("Request succeeded:\n\tsend: %s,\n\treceived: %s\n%n", sendData, receiveData);
    }

    static void logError(final String message) {
        System.err.println(message);
    }

    static void logError(final String message, final Exception e) {
        logError(message + e.getMessage());
    }

    static void shutdownAndAwaitTermination(final ExecutorService service) {
        service.shutdownNow();
        try {
            while (!service.awaitTermination(1, TimeUnit.MINUTES)) {
                logError("Service did not terminate");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static String getData(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(),
                packet.getLength(), StandardCharsets.UTF_8);
    }

    static String buildMessage(final String prefix, final int threadId, final int requestId) {
        return prefix + threadId + '_' + requestId;
    }

    static ByteBuffer allocate(final DatagramChannel channel) throws SocketException {
        return ByteBuffer.allocate(channel.socket().getReceiveBufferSize());
    }

    static boolean checkMessage(final String message, final int threadId, final int requestId) {
        final Matcher matcher = pattern.matcher(message);
        return matcher.matches()
                && String.valueOf(threadId).equals(matcher.group(1))
                && String.valueOf(requestId).equals(matcher.group(2));
    }

    static void closeIfNotNull(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException ignored) {
                //  no operations
            }
        }
    }

    static void shutdownIfNotNull(final ExecutorService service) {
        if (service != null) {
            shutdownAndAwaitTermination(service);
        }
    }

    static String getBufferContent(final ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

    static String buildResponse(final String request) {
        return "Hello, " + request;
    }

    static void closeChannel(final DatagramChannel channel) {
        try {
            channel.close();
        } catch (final IOException e1) {
            logError("Cannot close channel", e1);
        }
    }

    static void send(final DatagramChannel channel, final ByteBuffer buffer, final SocketAddress address) {
        try {
            channel.send(buffer, address);
        } catch (final IOException e) {
            closeChannel(channel);
        }
    }

    static SocketAddress receive(final DatagramChannel channel, final ByteBuffer buffer) {
        try {
            return channel.receive(buffer);
        } catch (IOException e) {
            closeChannel(channel);
            return null;
        }
    }

    static byte[] getBytes(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    static <T extends HelloServer> void startServer(final String[] args, final String name, final Supplier<T> supplier) {
        if (args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Usage: " + name + " <port> <threads>, arguments shouldn't be null");
            return;
        }
        try (final T server = supplier.get()) {
            server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (final NumberFormatException e) {
            System.out.println("Arguments should be integers");
        }
    }

    static <T extends HelloClient> void startClient(final String[] args, final String name, final Supplier<T> supplier) {
        if (args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.out.println("Usage: " + name + " <host> <port> <prefix> <threads> <requests>, "
                    + "arguments shouldn't be null");
            return;
        }
        try {
            supplier.get().run(args[0], Integer.parseInt(args[1]), args[2],
                    Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (final NumberFormatException e) {
            System.out.println("Arguments <port>, <threads> and <requests> should be integers");
        }
    }
}
