package ru.nsu.ccfit.boltava.socket;

import ru.nsu.ccfit.boltava.socket.segment.TouSegment;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import static ru.nsu.ccfit.boltava.socket.TouProtocolUtils.MAX_SEGMENT_BODY_LENGTH;

class TouSender extends Thread {

    // Size of the buffer, used to store data to be sent
    private static final int BUFFER_SIZE = 64 * 1000;
    // Max size of the queue of pending TOU segments
    private static final int MAX_QUEUE_SIZE = 20;
    // milliseconds to wait after first byte has arrived into buffer
    private static final int WAIT_TIMEOUT = 100;

    private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<TouSegment> pendingSegments = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);

    private DatagramSocket socket;
    private int sequenceNumber = 0;
    private int ackNumber = 0;

    private final Object lock = new Object();

    TouSender(DatagramSocket socket) {
        super("Sender");
        this.socket = socket;
    }

    @Override
    public void run() {
        final String threadName = Thread.currentThread().getName();
        System.out.println(threadName + " has started");
        while (!Thread.interrupted()) {
            try {
                System.out.println(threadName + " Trying");
                if (!pendingSegments.isEmpty()) {
                    System.out.println("Sending segments");
                    sendSegments();
                } else {
                    synchronized (lock) {
                        System.out.println(threadName + " Here");
                        while (buffer.position() == 0 && pendingSegments.isEmpty()) {
                            lock.wait();
                        }
                        // wait until sufficient number of bytes
                        // is added to buffer before slicing it
                        System.out.println(threadName + " Here 1");
                        lock.wait(WAIT_TIMEOUT);
                        sliceBuffer();
                        lock.notifyAll();
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to send segments");
                e.printStackTrace();
            } catch(InterruptedException e) {
                System.err.println("Sender interrupted");
//                e.printStackTrace();
                this.interrupt();
            }
        }
        System.err.println("Sender stopped");
    }

    public void sendSegment(TouSegment segment) throws IOException {
        synchronized (lock) {
            segment.setAckNumber(ackNumber);
            segment.setSequenceNumber(sequenceNumber);
            byte[] payload = segment.toBytes();
            socket.send(new DatagramPacket(payload, payload.length, segment.getAddress()));
            lock.notifyAll();
            System.out.println("Sent segment");
        }
    }

    public void appendSegment(TouSegment segment) {
        synchronized (lock) {
            pendingSegments.add(segment);
            System.out.println("Appended segment");
            lock.notifyAll();
        }
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

    public void updateAckNumber(int ackNumber) {
        synchronized (lock) {
            System.out.println("Updating ACK number");
            this.ackNumber = ackNumber;
        }
    }

    public void updateSequenceNumber(int seqNumber) {
        synchronized (lock) {
            System.out.println("Updating SEQ number");
            this.sequenceNumber = seqNumber;
            for (TouSegment segment : pendingSegments) {
                if (segment.getSequenceNumber() + segment.getPayload().length <= seqNumber) {
                    sequenceNumber = segment.getSequenceNumber() + segment.getPayload().length;
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
            int curSequenceNumber = sequenceNumber;
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
