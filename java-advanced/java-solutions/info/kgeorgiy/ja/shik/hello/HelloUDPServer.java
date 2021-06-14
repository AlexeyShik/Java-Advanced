package info.kgeorgiy.ja.shik.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService service;

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
            socket = new DatagramSocket(port);
            service = Executors.newFixedThreadPool(threads);
            for (int i = 0; i < threads; ++i) {
                service.submit(this::processRequest);
            }
        } catch (final SocketException e) {
            HelloUtils.logError("Cannot open socket: ", e);
        }
    }

    private void processRequest() {
        try {
            final int bufferSize = socket.getReceiveBufferSize();
            final DatagramPacket packet = new DatagramPacket(new byte[bufferSize], bufferSize);

            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    socket.receive(packet);
                    packet.setData(HelloUtils.buildResponse(HelloUtils.getData(packet)).getBytes(StandardCharsets.UTF_8));
                    socket.send(packet);
                } catch (final IOException ignored) {
                    //  no operations
                }
            }
        } catch (final SocketException ignored) {
            //  no operations
        }
    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        socket.close();
        HelloUtils.shutdownAndAwaitTermination(service);
    }

    /**
     * Main function for {@code HelloUDPServer}
     *
     * @param args <ul>
     *                 <li>
     *                     port - server port.
     *                 </li>
     *                 <li>
     *                     threads - number of working threads.
     *                 </li>
     *             </ul>
     */
    public static void main(final String[] args) {
        HelloUtils.startServer(args, "HelloUDPServer", HelloUDPServer::new);
    }
}
