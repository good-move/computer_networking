package ru.nsu.ccfit.boltava.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static ru.nsu.ccfit.boltava.socket.TouProtocolUtils.MAX_SEGMENT_BODY_LENGTH;

class TouSender implements Runnable {

    // Size of the buffer, used to store data to be sent
    private static final int BUFFER_SIZE = 64 * 1000;
    // Max size of the queue of pending TOU segments
    private static final int MAX_QUEUE_SIZE = 20;
    // milliseconds to wait after first byte has arrived into buffer
    private static final int WAIT_TIMEOUT = 100;

    private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<TouSegment> pendingSegments = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

    private DatagramSocket socket;
    private long sequenceNumber = 0L;
    private int ackNumber;

    private final Object lock = new Object();

    TouSender(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                if (!pendingSegments.isEmpty()) {
                    sendSegments();
                } else {
                    synchronized (lock) {
                        while (buffer.position() == 0) {
                            lock.wait();
                        }
                        // wait until sufficient number of bytes
                        // is added to buffer before slicing it
                        lock.wait(WAIT_TIMEOUT);
                        sliceBuffer();
                        notifyAll();
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to send segments");
                e.printStackTrace();
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void sendSegment(TouSegment segment) throws IOException {
        synchronized (lock) {
            byte[] payload = segment.toBytes();
            socket.send(new DatagramPacket(payload, payload.length, segment.getAddress()));
            lock.notifyAll();
        }
    }

    public void appendSegment(TouSegment segment) {
        pendingSegments.add(segment);
    }

    public void appendByte(byte payload) throws InterruptedException {
        synchronized (lock) {
            while (buffer.position() == buffer.limit()) {
                lock.wait();
            }
            buffer.put(payload);
            lock.notifyAll();
        }
    }

    public void setAckNumber(int ackNumber) {
        synchronized (lock) {
            this.ackNumber = ackNumber;
            for (TouSegment segment : pendingSegments) {
                if (segment.getSequenceNumber() <= ackNumber) {
                    pendingSegments.remove(segment);
                }
            }
        }
    }

    private void sliceBuffer() throws InterruptedException {
        synchronized (lock) {
            while (buffer.position() == 0) {
                lock.wait();
            }

            byte[] bufferToSlice = this.buffer.array();
            int curSequenceNumber = (int)sequenceNumber;
            int offset = 0;

            while (offset < bufferToSlice.length) {
                byte[] payload = new byte[Math.min(bufferToSlice.length - offset, MAX_SEGMENT_BODY_LENGTH)];
                this.buffer.get(payload, offset, payload.length);
                TouSegment segment = new TouSegment(
                        new InetSocketAddress(socket.getInetAddress(), socket.getPort()),
                        payload,
                        payload.length
                );
                segment.setSequenceNumber(curSequenceNumber);
                segment.setAckNumber(ackNumber);
                segment.changeAckFlag(true);
                pendingSegments.add(segment);
                curSequenceNumber += payload.length;
            }
            this.buffer.clear();
        }
    }

    private void sendSegments() throws IOException {
        synchronized (lock) {
            for (TouSegment segment : pendingSegments) {
                sendSegment(segment);
            }
        }
    }

}
