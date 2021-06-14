package info.kgeorgiy.ja.shik.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HelloUDPNonblockingClient implements HelloClient {

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
        final SocketAddress address;
        if ((address = HelloUtils.validateAndGetAddress(host, port, threads, requests)) == null) {
            return;
        }
        final List<DatagramChannel> channels = new ArrayList<>();
        try (final Selector selector = Selector.open()) {
            int processedThreads = 0;
            final ByteBuffer[] buffers = new ByteBuffer[threads];
            final int[] requestId = new int[threads];
            for (int i = 0; i < threads; i++) {
                final DatagramChannel channel = DatagramChannel.open();
                channels.add(channel);
                channel.configureBlocking(false);
                channel.register(selector, SelectionKey.OP_WRITE, i);
                buffers[i] = HelloUtils.allocate(channel);
            }

            while (processedThreads < threads) {
                if (selector.select(HelloUtils.TIMEOUT) == 0) {
                    selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
                    continue;
                }
                for (final Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    final SelectionKey key = it.next();

                    final DatagramChannel channel = (DatagramChannel) key.channel();
                    final int threadId = (int) key.attachment();
                    if (key.isWritable()) {
                        final String message = HelloUtils.buildMessage(prefix, threadId, requestId[threadId]);
                        channel.send(ByteBuffer.wrap(HelloUtils.getBytes(message)), address);
                        System.out.println("Send message: " + message);
                        key.interestOps(SelectionKey.OP_READ);
                    } else {
                        final ByteBuffer buffer = buffers[threadId];
                        buffer.clear();
                        channel.receive(buffer);
                        buffer.flip();
                        final String message = HelloUtils.getBufferContent(buffer);
                        System.out.println("Receive message: " + message);
                        key.interestOps(SelectionKey.OP_WRITE);

                        if (HelloUtils.checkMessage(message, threadId, requestId[threadId])) {
                            if (++requestId[threadId] == requests) {
                                processedThreads++;
                                channel.close();
                            }
                        }
                    }
                    it.remove();
                }
            }
        } catch (final IOException e) {
            HelloUtils.logError("I/O exception occurred", e);
            for (final DatagramChannel channel : channels) {
                try {
                    channel.close();
                } catch (final IOException ignored) {
                    //  no operations
                }
            }
        }
    }

    /**
     * Main function for HelloUDPNonblockingClient
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
        HelloUtils.startClient(args, "HelloUDPNonblockingClient", HelloUDPNonblockingClient::new);
    }
}
