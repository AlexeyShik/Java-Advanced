package info.kgeorgiy.ja.shik.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPClient implements HelloClient {

    /**
     * Runs Hello client.
     * This method should return when all requests completed.
     *
     * @param host     server host.
     * @param port     server port.
     * @param prefix   request prefix.
     * @param threads  number of request threads.
     * @param requests number of requests per thread.
     */
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        SocketAddress address;
        if ((address = HelloUtils.validateAndGetAddress(host, port, threads, requests)) == null) {
            return;
        }
        final ExecutorService service = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; ++i) {
            final int finalI = i;
            service.submit(() -> HelloUDPClient.makeRequest(address, prefix, requests, finalI));
        }
        HelloUtils.shutdownAndAwaitTermination(service);
    }

    private static void makeRequest(final SocketAddress address, final String prefix,
                                    final int requests, final int threadId) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(HelloUtils.TIMEOUT);
            final int bufferSize = socket.getReceiveBufferSize();
            final DatagramPacket responsePacket = new DatagramPacket(new byte[bufferSize], bufferSize);
            final DatagramPacket requestPacket = new DatagramPacket(new byte[bufferSize], bufferSize, address);
            for (int requestId = 0; requestId < requests; ++requestId) {
                final String requestData = HelloUtils.buildMessage(prefix, threadId, requestId);
                requestPacket.setData(requestData.getBytes(StandardCharsets.UTF_8));

                String responseData = "";
                do {
                    try {
                        socket.send(requestPacket);
                    } catch (final IOException e) {
                        HelloUtils.logError("Failed to send data: ", e);
                        continue;
                    }
                    try {
                        socket.receive(responsePacket);
                    } catch (final IOException e) {
                        HelloUtils.logError("Failed to receive data: ", e);
                        continue;
                    }
                    responseData = HelloUtils.getData(responsePacket);
                } while (!socket.isClosed() && !HelloUtils.checkMessage(responseData, threadId, requestId));

                if (socket.isClosed()) {
                    HelloUtils.logError("Request failed: socket is closed");
                } else {
                    HelloUtils.logInfo(requestData, responseData);
                }
            }
        } catch (final SocketException e) {
            HelloUtils.logError("Cannot open socket:  ", e);
        }
    }

    /**
     * Main function for HelloUDPClient
     *
     * @param args <ul>
     *                  <li>
     *                      host - server host.
     *                  </li>
     *                  <li>
     *                      port - server port.
     *                  </li>
     *                  <li>
     *                      prefix - request prefix.
     *                  </li>
     *                  <li>
     *                      threads - number of request threads.
     *                  </li>
     *                  <li>
     *                      requests - number of requests per thread.
     *                  </li>
     *             </ul>
     */
    public static void main(final String[] args) {
        HelloUtils.startClient(args, "HelloClient", HelloUDPClient::new);
    }
}
