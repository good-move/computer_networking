package ru.nsu.ccfit.boltava.socket;

import ru.nsu.ccfit.boltava.socket.segment.TouSegment;
import ru.nsu.ccfit.boltava.socket.segment.TouSynAckSegment;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class TouServerSocket implements AutoCloseable {

    private BlockingQueue<InetSocketAddress> acceptedConnections = new LinkedBlockingDeque<>();
    private HashMap<InetSocketAddress, TouSocket> clients = new HashMap<>();
    private HashSet<InetSocketAddress> pendingConnections = new HashSet<>();

    private TouProtocolUtils.SocketState state;
    private DatagramSocket socket;
    private TouReceiver receiver;
    private TouSender sender;

    private boolean isListening = false;

    private final Object lock = new Object();

    public TouServerSocket() {}

    public TouServerSocket(int port) throws SocketException, UnknownHostException {
        bind(port);
        listen();
    }

    public void bind(int port) throws SocketException, UnknownHostException {
        socket = new DatagramSocket(port);
        socket.setReuseAddress(true);
    }

    public void listen() {
        if (isListening) throw new IllegalStateException("Already in the listening state");
        if (state == TouProtocolUtils.SocketState.CLOSED)
            throw new IllegalStateException("Socket is closed");

        isListening = true;

        sender = new TouSender(socket);
        receiver = new TouReceiver(socket, new SegmentsHandler());
        sender.start();
        receiver.start();
    }

    public void close() {
        if (!isListening) throw new IllegalStateException("Socket is not listening");
        if (state == TouProtocolUtils.SocketState.CLOSED) return;
        sender.interrupt();
        receiver.interrupt();
        pendingConnections.clear();
        acceptedConnections.clear();
        socket.close();
        state = TouProtocolUtils.SocketState.CLOSED;
    }

    public TouSocket accept() throws IOException {
        if (state == TouProtocolUtils.SocketState.CLOSED) throw new IOException("Socket is closed");

        InetSocketAddress address;
        try {
            //    TODO: make waiting threads wake up when this.close() is invoked
            address = acceptedConnections.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Interrupted while waiting for incoming connections");
        }

        TouSocket client = new TouSocket(this, address.getAddress(), address.getPort());
        clients.put(address, client);
        return client;
    }

    public int getLocalPort() {
        return socket.getPort();
    }
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    DatagramSocket getSocket() {
        return socket;
    }

    void removeClient(InetSocketAddress address) {
        clients.remove(address);
    }

    private class SegmentsHandler implements ISegmentsHandler {

        @Override
        public void onAckReceived(TouSegment segment) {
            System.out.println("ServerSocket received ACK");
            sender.updateSequenceNumber(receiver.getAckNumber());
            synchronized (lock) {
                if (pendingConnections.contains(segment.getAddress())) {
                    pendingConnections.remove(segment.getAddress());
                    acceptedConnections.add(segment.getAddress());
                    sender.updateSequenceNumber((int) receiver.getAckNumber());
                }
            }
        }

        @Override
        public void onSynReceived(TouSegment segment) {
            System.out.println("ServerSocket received SYN");
            sender.updateAckNumber(receiver.getAckNumber());
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
