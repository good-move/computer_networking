package ru.nsu.ccfit.boltava.socket;

import ru.nsu.ccfit.boltava.socket.segment.TouAckSegment;
import ru.nsu.ccfit.boltava.socket.segment.TouFinSegment;
import ru.nsu.ccfit.boltava.socket.segment.TouSegment;
import ru.nsu.ccfit.boltava.socket.segment.TouSynSegment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;


public class TouSocket {

    private DatagramSocket localSocket;
    private InetAddress address;
    private int port;

    private boolean isBound = false;
    private boolean isConnected = false;

    private int sequenceNumber = 0;
    private int ackNumber = 0;
    private TouProtocolUtils.SocketState state;
    private TouSender sender;
    private TouReceiver receiver;
    private SegmentsHandler handler = new SegmentsHandler();

    private final Object lock = new Object();

    private Origin origin = Origin.CLIENT;
    private TouServerSocket serverSocket = null;

    public TouSocket() {}

    public TouSocket(InetAddress address, int port) throws IOException {
        localSocket = new DatagramSocket();
        isBound = true;
        connect(new InetSocketAddress(address, port));
    }

    TouSocket(TouServerSocket serverSocket, InetAddress address, int port) {
        this.origin = Origin.SERVER;

        this.address = address;
        this.port = port;
        localSocket = serverSocket.getSocket();
        sender = new TouSender(localSocket);
        receiver = new TouReceiver(localSocket, handler);
        start();
        state = TouProtocolUtils.SocketState.ESTABLISHED;
    }

    public void bind(SocketAddress bindPoint) throws IOException {
        if (this.isBound) throw new IOException("Socket is already bound");

        this.localSocket = new DatagramSocket(bindPoint);
        this.isBound = true;
    }

    public void connect(SocketAddress address) throws IOException {
        if (address == null) throw new IllegalArgumentException("Cannot connect to null address");

        this.address = ((InetSocketAddress) address).getAddress();
        this.port = ((InetSocketAddress) address).getPort();
        sender = new TouSender(localSocket);
        receiver = new TouReceiver(localSocket, handler);
        start();
        sender.sendSegment(
            new TouSynSegment(new InetSocketAddress(this.address, port), sequenceNumber, ackNumber)
        );
        state = TouProtocolUtils.SocketState.SYN_SENT;
    }

    public void close() throws IOException {
        if (!isConnected) throw new RuntimeException("Socket is not connected");
        if (state == TouProtocolUtils.SocketState.CLOSED) return;
        sender.sendSegment(
            new TouFinSegment(new InetSocketAddress(this.address, port), sequenceNumber, ackNumber)
        );

    }

    public SocketAddress getRemoteSocketAddress() {
        return new InetSocketAddress(address, port);
    }

    public InetAddress getInetAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getLocalPort() {
        return localSocket.getPort();
    }

    public InetAddress getLocalAddress() {
        return localSocket.getInetAddress();
    }

    public InputStream getInputStream() {
        return null;
    }

    public OutputStream getOutputStream() {
        return null;
    }


    private void start() {
        sender.start();
        receiver.start();
    }

    private class SegmentsHandler implements ISegmentsHandler {

        public void onAckReceived(TouSegment segment) {
            sender.updateSequenceNumber(segment.getSequenceNumber());
            sender.updateAckNumber(receiver.getAckNumber());
            switch (state) {
                case ESTABLISHED:
                    break;
                case CLOSING:
                    serverSocket.removeClient(new InetSocketAddress(address, port));
                    state = TouProtocolUtils.SocketState.CLOSED;
                    break;
            }
        }

        public void onSynReceived(TouSegment segment) {
            System.err.println("Client socket received SYN segment");
        }

        public void onFinReceived(TouSegment segment) {
            sender.updateSequenceNumber(receiver.getAckNumber());
            try {
                sender.sendSegment(new TouAckSegment(new InetSocketAddress(address, port)));
            } catch (IOException e) {
                System.err.println("Failed to respond with ACK");
                e.printStackTrace();
            }
        }

        public void onSynAckReceived(TouSegment segment) {
            sender.updateSequenceNumber(segment.getAcknowledgementNumber());
            sender.updateAckNumber(receiver.getAckNumber());
            if (state == TouProtocolUtils.SocketState.SYN_SENT) {
                isConnected = true;
                state = TouProtocolUtils.SocketState.ESTABLISHED;
                System.out.println("connection ESTABLISHED");
                try {
                    sender.sendSegment(new TouAckSegment(new InetSocketAddress(address, port)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private enum Origin {
        CLIENT,
        SERVER
    }

}
