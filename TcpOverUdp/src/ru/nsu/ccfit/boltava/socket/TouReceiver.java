package ru.nsu.ccfit.boltava.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

class TouReceiver implements Runnable {

    static private final int MAX_BUFFER_SIZE = 64 * 1000;

    private ByteBuffer dataBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
    private long ackNumber = 0;
    private final DatagramSocket socket;

    TouReceiver(DatagramSocket socket) {
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
                if (ackNumber < segment.getSequenceNumber() + segment.getPayload().length) {
                    handleSegment(segment);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void handleSegment(TouSegment segment) {
        switch (segment.getType()) {
            case SYN:
                break;
            case SYNACK:
                break;
            case ACK:
                break;
            case FIN:
                break;
            default:
                throw new IllegalStateException("Unknown segment type: " + segment.getType().toString());
        }
    }

}
