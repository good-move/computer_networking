package ru.nsu.ccfit.boltava.socket;

import ru.nsu.ccfit.boltava.socket.segment.TouSegment;
import ru.nsu.ccfit.boltava.socket.segment.TouSynAckSegment;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class TouServerSocket {

    private BlockingQueue<InetSocketAddress> acceptedConnections = new LinkedBlockingDeque<>();
    private HashSet<InetSocketAddress> pendingConnections = new HashSet<>();

    private TouProtocolUtils.SocketState state;
    private DatagramSocket address;
    private TouReceiver receiver;
    private TouSender sender;

    private final Object lock = new Object();

    public TouServerSocket(int port) throws SocketException {
        bind(port);
        listen();
    }

    public void bind(int port) throws SocketException {
        address = new DatagramSocket(port);
    }

    public void listen() {
        sender = new TouSender(address);
        receiver = new TouReceiver(address, new SegmentsHandler());
        sender.start();
        receiver.start();
    }

    public void close() {
        if (state == TouProtocolUtils.SocketState.CLOSED) return;
        sender.interrupt();
        receiver.interrupt();
        pendingConnections.clear();
        acceptedConnections.clear();
        state = TouProtocolUtils.SocketState.CLOSED;
    }

    public TouSocket accept() throws IOException {
        if (state == TouProtocolUtils.SocketState.CLOSED) throw new IOException("Socket is closed");

        InetSocketAddress address;
        try {
            address = acceptedConnections.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Interrupted waiting for incoming connections");
        }

        return new TouSocket(address.getAddress(), address.getPort());
    }

    private class SegmentsHandler implements ISegmentsHandler {

        @Override
        public void onAckReceived(TouSegment segment) {
            synchronized (lock) {
                if (pendingConnections.contains(segment.getAddress())) {
                    pendingConnections.remove(segment.getAddress());
                    acceptedConnections.add(segment.getAddress());
                    sender.updateAckNumber((int) receiver.getAckNumber());
                }
            }
        }

        @Override
        public void onSynReceived(TouSegment segment) {
            synchronized (lock) {
                try {
                    if (!pendingConnections.contains(segment.getAddress())) {
                        pendingConnections.add(segment.getAddress());
                        sender.sendSegment(new TouSynAckSegment(segment.getAddress()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        }

        @Override
        public void onSynAckReceived(TouSegment segment) {
            System.err.println("ServerSocket received SYN-ACK");
        }

        @Override
        public void onFinReceived(TouSegment segment) {
            System.err.println("ServerSocket received FIN");
        }
    }

}