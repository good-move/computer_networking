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
        this.socket = socket;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                byte[] segmentBytes = new byte[
                    TouProtocolUtils.SEGMENT_HEADER_LENGTH + TouProtocolUtils.MAX_SEGMENT_BODY_LENGTH
                ];
                DatagramPacket packet = new DatagramPacket(segmentBytes, segmentBytes.length);
                socket.receive(packet);
                TouSegment segment = new TouSegment(
                    new InetSocketAddress(packet.getAddress(), packet.getPort()),
                    packet.getData(),
                    packet.getLength()
                );
                // ignore packets, which already have been acknowledged
                if (totalBytesReceived < segment.getSequenceNumber() + segment.getPayload().length) {
                    handleSegment(segment);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
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

    public long getAckNumber() {
        return totalBytesReceived;
    }

    private void handleSegment(TouSegment segment) {
        switch (segment.getType()) {
            case SYN:
                totalBytesReceived = segment.getSequenceNumber() + 1;
                handler.onSynReceived(segment);
                break;
            case SYNACK:
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
