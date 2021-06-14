package info.kgeorgiy.ja.shik.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer implements HelloServer {

    private Selector selector;
    private DatagramChannel selectorChannel;
    private ExecutorService service;

    private void response(final SocketAddress address, final ByteBuffer buffer, final ChannelInfo context) {
        buffer.flip();
        final byte[] response = HelloUtils.getBytes(HelloUtils.buildResponse(HelloUtils.getBufferContent(buffer)));
        buffer.clear();
        buffer.put(response);
        buffer.flip();
        context.addResponse(buffer, address);
    }

    private void run() {
        while (selector.isOpen()) {
            try {
                selector.select();
                for (final Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    final SelectionKey key = it.next();
                    final DatagramChannel channel = (DatagramChannel) key.channel();
                    final ChannelInfo context = (ChannelInfo) key.attachment();
                    if (key.isWritable()) {
                        final ChannelInfo.BufferAndAddress response = context.getResponse();
                        final ByteBuffer buffer = response.getBuffer();
                        HelloUtils.send(channel, buffer, response.getAddress());
                        context.addBuffer(buffer.clear());
                    }
                    if (key.isReadable()) {
                        final ByteBuffer buffer = context.getBuffer();
                        final SocketAddress address = HelloUtils.receive(channel, buffer);
                        if (address != null) {
                            service.submit(() -> response(address, buffer, context));
                        }
                    }
                    it.remove();
                }
            } catch (final IOException e) {
                HelloUtils.logError("Cannot select keys", e);
            }
        }
    }


    /**
     * Starts a new Hello server.
     * This method should return immediately.
     *
     * @param port    server port.
     * @param threads number of working threads.
     */
    @Override
    public void start(final int port, final int threads) {
        if (!HelloUtils.validatePortAndThreads(port, threads)) {
            return;
        }
        try {
            selector = Selector.open();
            selectorChannel = DatagramChannel.open();
            selectorChannel.configureBlocking(false);
            selectorChannel.bind(new InetSocketAddress(port));
            selectorChannel.register(selector, SelectionKey.OP_READ, new ChannelInfo(threads, selectorChannel));

            // :NOTE: Два executor - fixed
            service = Executors.newFixedThreadPool(threads + 1);

            service.submit(this::run);
        } catch (final IOException e) {
            HelloUtils.logError("Cannot start server", e);
        }
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        HelloUtils.closeIfNotNull(selector);
        HelloUtils.closeIfNotNull(selectorChannel);
        HelloUtils.shutdownIfNotNull(service);
    }

    private class ChannelInfo {
        private final Queue<BufferAndAddress> responses;
        private final Queue<ByteBuffer> freeBuffers;

        private ChannelInfo(final int queueSize, final DatagramChannel channel) throws SocketException {
            responses = new LinkedList<>();
            freeBuffers = new LinkedList<>();
            for (int i = 0; i < queueSize; ++i) {
                freeBuffers.add(HelloUtils.allocate(channel));
            }
        }

        private synchronized void addBuffer(final ByteBuffer buffer) {
            if (freeBuffers.isEmpty() &&
                    (selectorChannel.keyFor(selector).interestOps() & SelectionKey.OP_READ) == 0) {
                setKeyOpsOr(SelectionKey.OP_READ);
            }
            freeBuffers.add(buffer);
        }

        private synchronized ByteBuffer getBuffer() {
            if (freeBuffers.size() == 1) {
                setKeyOpsAnd(~SelectionKey.OP_READ);
            }
            return freeBuffers.poll();
        }

        private synchronized void addResponse(final ByteBuffer buffer, final SocketAddress address) {
            if (responses.isEmpty() &&
                    (selectorChannel.keyFor(selector).interestOps() & SelectionKey.OP_WRITE) == 0) {
                setKeyOpsOr(SelectionKey.OP_WRITE);
            }
            responses.add(new BufferAndAddress(buffer, address));
        }

        private synchronized BufferAndAddress getResponse() {
            if (responses.size() == 1) {
                setKeyOpsAnd(~SelectionKey.OP_WRITE);
            }
            return responses.poll();
        }

        private void setKeyOpsAnd(final int ops) {
            selectorChannel.keyFor(selector).interestOpsAnd(ops);
        }

        private void setKeyOpsOr(final int ops) {
            selectorChannel.keyFor(selector).interestOpsOr(ops);
            selector.wakeup(); // :NOTE: Слишком часто - fixed
        }

        private class BufferAndAddress {
            private final ByteBuffer buffer;
            private final SocketAddress address;

            private BufferAndAddress(final ByteBuffer buffer, final SocketAddress address) {
                this.buffer = buffer;
                this.address = address;
            }

            public ByteBuffer getBuffer() {
                return buffer;
            }

            public SocketAddress getAddress() {
                return address;
            }
        }
    }

    /**
     * Main function for {@code HelloUDPNonblockingServer}
     *
     * @param args <ul>
     *             <li>
     *             port - server port.
     *             </li>
     *             <li>
     *             threads - number of working threads.
     *             </li>
     *             </ul>
     */
    public static void main(final String[] args) {
        HelloUtils.startServer(args, "HelloUDPNonblockingServer", HelloUDPNonblockingServer::new);
    }
}