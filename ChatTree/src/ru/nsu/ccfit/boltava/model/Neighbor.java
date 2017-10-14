package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.event.EventDispatcher;
import ru.nsu.ccfit.boltava.model.event.IEventListener;
import ru.nsu.ccfit.boltava.model.event.AckReceivedEvent;
import ru.nsu.ccfit.boltava.model.message.Message;
import ru.nsu.ccfit.boltava.model.net.DatagramMessageSender;
import ru.nsu.ccfit.boltava.model.serializer.XmlMessageSerializer;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Neighbor represents a Client's adjacent node.
 * The class provides message sending interface.
 */
public class Neighbor {

    private static final int QUEUE_SIZE = 500;

    private EventDispatcher<AckReceivedEvent> eventDispatcher;
    private LinkedBlockingQueue<ExecutionPack> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private Thread worker;

    private final InetSocketAddress address;
    private final DatagramSocket socket;

    public Neighbor(DatagramSocket socket, InetSocketAddress address, EventDispatcher<AckReceivedEvent> dispatcher)
            throws IOException, JAXBException {
        this.socket = socket;
        this.address = address;
        this.eventDispatcher = dispatcher;
        worker = new Thread(new MessageSender(), "Neighbor");
        worker.start();
    }

    /**
     * Adds message to the message queue with some priorities:
     *  - ACK message has the highest priority
     *  - Other system messages have the same priority
     *  - Text messages have lowest priority
     *
     *  The only guarantee is that Message types with higher priority will
     *  likely be sent before those having lower priority score.
     *
     * @param message - message to send to the neighbor
     */
    public CompletableFuture<Message> sendMessage(Message message) throws InterruptedException {
        return feedQueue(message, null, null, null);
    }

    public CompletableFuture<Message> sendMessage(Message message, Runnable onSuccess, Runnable onError) throws InterruptedException {
        return feedQueue(message, onSuccess, onError, null);
    }

    public CompletableFuture<Message> sendMessage(Message message, Runnable onResult) throws InterruptedException {
        return feedQueue(message, null, null, onResult);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void detach() {
        if (!worker.isInterrupted()) {
            worker.interrupt();
            queue.clear();
        }
    }

    private CompletableFuture<Message> feedQueue(Message message,
                                                 Runnable onSuccess,
                                                 Runnable onError,
                                                 Runnable onAny) throws InterruptedException {
        if (message == null) {
            throw new IllegalArgumentException("Message can't be null");
        }
        CompletableFuture<Message> future = new CompletableFuture<>();

        queue.put(new ExecutionPack(message, onSuccess, onError, onAny, future));
//        System.out.println("OFFERED MESSAGE");

        return future;
    }

    private final class MessageSender implements Runnable, IEventListener<AckReceivedEvent> {

        private DatagramMessageSender sender;
        private HashSet<UUID> deliveredMessages = new HashSet<>();
        private LinkedBlockingQueue<ExecutionPack> pendingMessages = new LinkedBlockingQueue<>();
        private final long DELIVERY_INTERVAL = 100;
        private final long MAX_TTL = 5;
        private final long INFINITY = 0;

        MessageSender() throws IOException, JAXBException {
            sender = new DatagramMessageSender(socket, new XmlMessageSerializer());
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                ExecutionPack pack;
                if (pendingMessages.isEmpty()) {
//                    System.out.println("waiting for new incoming INF");
                    waitForIncoming(INFINITY);
                } else {
                    pack = pendingMessages.peek();
                    long diff = System.currentTimeMillis() - pack.sendTime + 1;
                    if (deliveredMessages.contains(pack.message.getId()) ||
                        pack.ttl >= MAX_TTL) {
                        pendingMessages.poll();
                        deliveredMessages.remove(pack.message.getId());
                        eventDispatcher.unsubscribe(
                            new AckReceivedEvent(address, pack.getMessage().getId()),
                            this
                        );
                        if (pack.ttl >= MAX_TTL) {
//                            System.out.println("trying to run onError");
                            if (pack.onError != null) pack.onError.run();
                            else if (pack.onResult != null) pack.onResult.run();
                            pack.future.cancel(true);
                        } else {
//                            System.out.println("trying to run onSuccess");
                            if (pack.onSuccess != null) pack.onSuccess.run();
                            else if (pack.onResult != null) pack.onResult.run();
                            pack.future.complete(pack.getMessage());
                        }
                    } else if (diff > DELIVERY_INTERVAL) {
                        pendingMessages.poll();
                        this.sendMessage(pack);
                    } else {
//                        System.out.println("waiting for incoming DIFF");
                        try {
                            pack = queue.poll(diff, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (pack == null) continue;
                        eventDispatcher.subscribe(
                                new AckReceivedEvent(address, pack.getMessage().getId()),
                                this
                        );
                        this.sendMessage(pack);
                    }
                }
            }
        }

        private void waitForIncoming(long timeoutMillis) {
            ExecutionPack pack;
            try {
                if (timeoutMillis != INFINITY) {
                    pack = queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
                } else {
                    pack = queue.take();
                }
                if (pack == null) return;
                eventDispatcher.subscribe(
                    new AckReceivedEvent(address, pack.getMessage().getId()),
                    this
                );
                this.sendMessage(pack);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.err.println("Failed to fetch message from queue");
            }
        }

        private void sendMessage(ExecutionPack pack) {
            try {
//                System.out.println("Sending message");
                sender.send(pack.getMessage(), address);
                pack.sendTime = System.currentTimeMillis();
                pack.ttl++;
                pendingMessages.put(pack);
            } catch (IOException | JAXBException e) {
                e.printStackTrace();
                System.err.println(
                    "Failed to send message of type " + pack.getMessage().getClass().getSimpleName()
                );
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void act(AckReceivedEvent event) {
            System.out.println("Received ACK from " + address);
            deliveredMessages.add(event.getMessageId());
        }

    }

    private final class ExecutionPack {

        private final Message message;
        private final Runnable onSuccess;
        private final Runnable onError;
        private final Runnable onResult;
        private final CompletableFuture<Message> future;
        private long sendTime;
        private int ttl = 0;

        ExecutionPack(Message message,
                      Runnable onSuccess,
                      Runnable onError,
                      Runnable onResult, CompletableFuture<Message> future) {
            this.message = message;
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.onResult = onResult;
            this.future = future;
        }

        public Message getMessage() {
            return message;
        }

        public Runnable getOnSuccess() {
            return onSuccess;
        }

        public Runnable getOnError() {
            return onError;
        }

        public CompletableFuture<Message> getFuture() {
            return future;
        }

        public long getSendTime() {
            return sendTime;
        }

        public void setSendTime(long sendTime) {
            this.sendTime = sendTime;
        }
    }

}
