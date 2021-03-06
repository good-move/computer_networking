package ru.nsu.ccfit.boltava.socket;

import ru.nsu.ccfit.boltava.socket.segment.TouSegment;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

class TouReceiver extends Thread {

    static private final int MAX_BUFFER_SIZE = 64 * 1000;

    private byte[] buffer = new byte[MAX_BUFFER_SIZE];
    private int bufferEnd;
    private int bufferStart;

    private long totalBytesReceived = 0;
    private final DatagramSocket socket;
    private ISegmentsHandler handler;

    private final Object lock = new Object();

    TouReceiver(DatagramSocket socket, ISegmentsHandler handler) {
        super("Receiver");
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        final String THREAD_NAME = Thread.currentThread().getName();
        while (!Thread.interrupted()) {
            try {
                byte[] segmentBytes = new byte[
                    TouProtocolUtils.SEGMENT_HEADER_LENGTH + TouProtocolUtils.MAX_SEGMENT_BODY_LENGTH
                ];
                DatagramPacket packet = new DatagramPacket(segmentBytes, segmentBytes.length);
                System.out.println(THREAD_NAME + " waiting for segment");
                socket.receive(packet);
                System.out.println(THREAD_NAME + " received segment");
                TouSegment segment = new TouSegment(
                    new InetSocketAddress(packet.getAddress(), packet.getPort()),
                    packet.getData(),
                    packet.getLength()
                );
                System.out.println(segment.toString());
                // ignore packets, which already have been acknowledged
                if (totalBytesReceived <= segment.getSequenceNumber() + segment.getPayload().length) {
                    System.out.println(THREAD_NAME + " handling segment");
                    handleSegment(segment);
                }
            } catch (IOException e) {
                System.err.println("Receiver interrupted");
//                e.printStackTrace();
            }
        }
        System.err.println("Receiver stopped");
    }

    public int readByte() throws InterruptedException {
        int byteToReturn = 0;
        synchronized (lock) {
            while (bufferStart == bufferEnd) {
                lock.wait();
            }
            byteToReturn |= buffer[bufferStart];
            bufferStart++;
            // if all data has been read from buffer
            if (bufferStart == buffer.length) {
                bufferStart = 0;
                bufferEnd = 0;
            }
        }
        return byteToReturn;
    }

    int getAckNumber() {
        return (int) totalBytesReceived;
    }

    void shutdown() {
        this.interrupt();
    }

    private void handleSegment(TouSegment segment) {
        switch (segment.getType()) {
            case SYN:
                totalBytesReceived = segment.getSequenceNumber() + 1;
                handler.onSynReceived(segment);
                break;
            case SYNACK:
                totalBytesReceived = segment.getSequenceNumber() + 1;
                handler.onSynAckReceived(segment);
                break;
            case ACK:
                if (segment.getPayload().length > 0) {
                    tryAppendSegment(segment);
                    return;
                }

                handler.onAckReceived(segment);
                break;
            case FIN:
                handler.onFinReceived(segment);
                break;
            default:
                throw new IllegalStateException("Unknown segment type: " + segment.getType().toString());
        }
    }

    private void tryAppendSegment(TouSegment segment) {
        synchronized (lock) {
            final byte[] payload = segment.getPayload();
            if (bufferEnd + payload.length < buffer.length &&
                totalBytesReceived == segment.getSequenceNumber()) {
                System.arraycopy(buffer, bufferEnd, payload, 0, payload.length);
                bufferEnd += payload.length;
                totalBytesReceived += payload.length;
            }
            lock.notifyAll();
        }
    }

}
