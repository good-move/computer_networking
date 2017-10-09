package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.event.AckReceivedEvent;
import ru.nsu.ccfit.boltava.model.event.EventDispatcher;
import ru.nsu.ccfit.boltava.model.message.*;
import ru.nsu.ccfit.boltava.model.net.DatagramMessageReceiver;
import ru.nsu.ccfit.boltava.model.serializer.XmlMessageSerializer;
import ru.nsu.ccfit.boltava.view.IMessageListener;
import ru.nsu.ccfit.boltava.view.IMessageRenderer;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public final class Client implements IMessageListener {

    private static int cacheSize = 50;
    private static EventDispatcher<AckReceivedEvent> eventDispatcher = new EventDispatcher<>();

    private State state;
    private Node node;
    private final DatagramSocket socket;
//    private final int packetLossFactor;
    private HashSet<IMessageRenderer> renderers = new HashSet<>();
    private TreeSet<CacheEntry> cachedMessages = new TreeSet<>();

    private Thread messageListener;


    Client(String nodeName, int port, int packetLossFactor) throws IOException, JAXBException {
        this(nodeName, port, packetLossFactor, null);
    }

    Client(String nodeName, int port, int packetLossFactor, InetSocketAddress parentAddress)
            throws IOException, JAXBException {
        System.out.println("Initializing node...");

        this.socket = new DatagramSocket(port);
        Neighbor parent = (parentAddress == null) ? null : new Neighbor(this.socket, parentAddress, eventDispatcher);
        this.node =  parentAddress == null ? new Node(nodeName) : new Node(nodeName, parent);
        messageListener = new Thread(
                new MessageListener(socket, new MessageHandler(this, eventDispatcher), packetLossFactor),
                "Message Listener"
        );
        messageListener.start();
        if (!isRoot()) {
            state = State.JOINING;
            joinParent();
        } else {
            state = State.RUNNING;
        }

        setShutDownHook();
    }


//    ***************************** Public methods *****************************

    synchronized void addMessageRenderer(IMessageRenderer renderer) {
        renderers.add(renderer);
    }

    public synchronized void removeMessageRenderer(IMessageRenderer renderer) {
        renderers.remove(renderer);
    }

    synchronized void showMessage(TextMessage message) {
        renderers.forEach(r -> r.render(message));
    }

    synchronized Node getNode() {
        return node;
    }

    synchronized void setState(State state) {
        this.state = state;
    }

    synchronized State getState() {
        return state;
    }

    synchronized boolean isRoot() {
        return node.isRoot;
    }

    // ***************************** Message operation routines *****************************

    /**
     * Sends message to each neighbor except the message sender.
     *
     * @param message - message to broadcast
     */
    synchronized void broadcastMessage(Message message) throws InterruptedException {
        for (Neighbor neighbor : node.getNeighbors()) {
            if (!neighbor.getAddress().equals(message.getSender())) {
                neighbor.sendMessage(message);
            }
        }
    }

    /**
     * Sends message to the specified neighbor address. If no neighbor with such address
     * exists, nothing happens.
     *
     * @param message - message to send
     * @param receiver - address of the neighbor - messages receiver
     */
    synchronized void sendTo(Message message, InetSocketAddress receiver) throws InterruptedException {
        Neighbor neighbor = node.getNeighbor(receiver);
        if (neighbor != null) {
            neighbor.sendMessage(message);
        }
    }

    synchronized void sendTo(Message message, InetSocketAddress receiver, Runnable onSuccess, Runnable onError)
            throws InterruptedException {
        Neighbor neighbor = node.getNeighbor(receiver);
        if (neighbor != null) {
            neighbor.sendMessage(message, onSuccess, onError);
        }
    }

    @Override
    public void onTextMessageEntered(String message) {
        try {
            if (this.state == State.RUNNING) {
//                System.out.println("will broadcast message: " + message);
                broadcastMessage(new TextMessage(message, node.getName()));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("Client --- Failed to send text message: " + message);
        }
    }

    synchronized void registerMessage(Message message) {
        CacheEntry entry = new CacheEntry(System.currentTimeMillis(), message);
        if (!cachedMessages.contains(entry)) {
            if (cachedMessages.size() == cacheSize) {
                cleanCache(cacheSize / 10);
            }
//            System.out.println("ADDED CACHE ENTRY");
            cachedMessages.add(entry);
        }
    }

    synchronized boolean isMessageRegistered(Message message) {
        return cachedMessages.contains(new CacheEntry(0, message));
    }


    private void cleanCache(int entriesToRemove) {
        while (entriesToRemove-- > 0) {
            cachedMessages.pollFirst();
        }
    }

//    ***************************** Node modification methods *****************************

    /**
     * Removes parent connection for current node
     */
    synchronized void detachParent() {
        if (!isRoot()) {
            Neighbor neighbor = node.getParent();
            if (neighbor != null) {
                neighbor.detach();
                node.parent = null;
            }
            node.makeRoot();
        }
    }

    synchronized void setParent(InetSocketAddress address) throws IOException, JAXBException {
        if (isRoot()) {
            node.setParent(address);
        }
    }



//    ***************************** Internal state manipulation routines *****************************

    /**
     * Tries to send JOIN message to parent to establish logical connection
     */
    private void joinParent() {
        System.out.println("JOINING parent...");
        try {
            node.getParent()
                .sendMessage(
                    new JoinMessage(node.getName()),
                    () -> {
                        System.out.println("Joined parent!");
                        this.state = State.RUNNING;
                    }, () -> {
                        this.state = State.TERMINATED;
                        System.out.println("Failed to connect to parent");
                        messageListener.interrupt();
                        node.getParent().detach();
                    }
                );
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutDown, "ShutDown Hook"));
    }

    /**
     * Final method before the client is shut down.
     *
     * 1.1 If this client is not root, it's got a parent.
     * 1.2 Otherwise, it chooses one child to be new root and sends it a ROOT msg.
     * 2. Client sends REJOIN messages to all non-root children.
     * 3. When all children messages complete, it sends Leave msg to parent.
     * 4. Shuts down itself and associated view
     * 5. Calls exit.
     */
    private void shutDown() {
        this.setState(State.SHUTTING);
        // clean all msg queues
        if (isRoot()) {
            System.out.println(String.format("%s: shutdown", Thread.currentThread().getName()));
            Neighbor pseudoParent = null;
            ArrayList<Neighbor> children = node.getChildren();
            if (children.isEmpty()) {
                // shutDown fast
            } else {
                System.out.println(String.format("%s: looking for next root", Thread.currentThread().getName()));
                for (Neighbor child : node.getChildren()) {
                    try {
                        child.sendMessage(
                            new RootMessage(),
                                child::detach
                        ).get();
                        pseudoParent = child;
                        break;
                    } catch (InterruptedException | CancellationException | ExecutionException e) {
                        e.printStackTrace();
                        System.err.println(Thread.currentThread().getName() + ": child not available");
                    }
                }

                if (pseudoParent != null) {
                    System.out.println(String.format("%s: found next root", Thread.currentThread().getName()));
                    broadcastRejoin(pseudoParent);
                } else {
                    // shutdown fast
                    System.out.println(String.format("%s: didn't find next root", Thread.currentThread().getName()));
                }
            }
        } else {
            try {
                Neighbor parent = node.parent;
                broadcastRejoin(parent);
                parent.sendMessage(new LeaveMessage(), this::detachParent).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        setState(State.TERMINATED);
    }

    private void broadcastRejoin(Neighbor nextParent) {
        InetSocketAddress parentAddress = nextParent.getAddress();
        ArrayList<CompletableFuture<Message>> rejoinRequests = new ArrayList<>();
        for (Neighbor neighbor : node.getNeighbors()) {
            try {
                if (neighbor.getAddress().equals(nextParent.getAddress())) continue;

                rejoinRequests.add(neighbor.sendMessage(
                        new RejoinMessage(parentAddress.getAddress(), parentAddress.getPort()),
                        () -> {
                            neighbor.detach();
                            node.removeChild(neighbor.getAddress());
                        }
                ));
            } catch (InterruptedException e) {
                e.printStackTrace();
                rejoinRequests.get(rejoinRequests.size() - 1).cancel(true);
            }
        }

        CompletableFuture.allOf(
                rejoinRequests.toArray(new CompletableFuture[rejoinRequests.size()])
        ).join();
    }

    /**
     * MessageListener is supposed to wait for a udp packet to arrive,
     * registerMessage it as "received" and delegate it to an IMessageHandler
     * to process.
     *
     * NOTE: before passing the serializer over to an IMessageHandler, listener
     * first calls isPacketLost() to decide whether the current packet is
     * considered lost or not. If the former is true, the serializer never gets
     * registered nor processed.
     */
    private static final class MessageListener implements Runnable {

        MessageHandler handler;
        DatagramMessageReceiver receiver;
        private final int packetLossFactor;
        private final Random randGenerator = new Random();

        MessageListener(DatagramSocket socket, MessageHandler handler, int packetLossFactor) throws JAXBException {
            this.handler = handler;
            receiver = new DatagramMessageReceiver(socket, new XmlMessageSerializer());
            this.packetLossFactor = packetLossFactor;
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                try {
                    DatagramMessageReceiver.MessageWrapper wrapper = receiver.receive();
                    Message message = wrapper.getMessage();
                    if (!isPacketLost()) {
                        message.setSender(new InetSocketAddress(wrapper.getAddress(), wrapper.getPort()));
                        message.handle(handler);
                    }
                } catch (IOException | JAXBException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean isPacketLost() {
            return randGenerator.nextInt(100) <= packetLossFactor;
        }

    }

    /**
     * Represents client's state
     */
    public enum State {
        JOINING,
        RUNNING,
        SHUTTING,
        MOVING,
        TERMINATED
    }

    public final class Node {

        private boolean isRoot = true;
        private final String nodeName;
        private Neighbor parent;
        private ConcurrentHashMap<InetSocketAddress, Neighbor> children = new ConcurrentHashMap<>();

        Node(String nodeName) {
            this.nodeName = nodeName;
            isRoot = true;
        }

        Node(String name, Neighbor parent) {
            this.nodeName = name;
            this.parent = parent;
            isRoot = false;
        }

        String getName() {
            return nodeName;
        }

        /**
         * Adds child to children list. If such child already exists, nothing happens.
         * @param address - child to add
         */
        void addChild(InetSocketAddress address) throws IOException, JAXBException {
            if (!isRoot && parent.getAddress().equals(address)) {
                throw new IllegalArgumentException("Cannot assign parent to be a child");
            }
            if (!children.containsKey(address)) {
                children.put(address, new Neighbor(socket, address, eventDispatcher));
            }
        }

        /**
         * Removes specified child from children list. If such child doesn't exist,
         * nothing happens
         * @param address - child to remove
         */
        void removeChild(InetSocketAddress address) {
            Neighbor neighbor = children.remove(address);
            if (neighbor != null) {
                neighbor.detach();
            }
        }

        Neighbor getChild(InetSocketAddress address) {
            return children.get(address);
        }

        private void setParent(InetSocketAddress address) throws IOException, JAXBException {
            if (children.containsKey(address)) {
                throw new IllegalArgumentException("Cannot assign a child to be the parent");
            }
            this.parent = new Neighbor(socket, address, eventDispatcher);
            isRoot = false;
        }

        private void makeRoot() {
            isRoot = true;
        }

        Neighbor getParent() {
            return isRoot ? null : parent;
        }

        ArrayList<Neighbor> getChildren() {
            return new ArrayList<>(children.values());
        }

        ArrayList<Neighbor> getNeighbors() {
            ArrayList<Neighbor> neighbors = getChildren();
            if (!isRoot) {
                neighbors.add(parent);
            }
            return neighbors;
        }

        Neighbor getNeighbor(InetSocketAddress address) {
            if (!isRoot && parent.getAddress().equals(address)) {
                return parent;
            }
            return children.get(address);
        }

    }


    /**
     * CacheEntry represent Message arrival event in time. Message cache
     * is intended to prevent multiple reactions to the same received message
     * even if the ACK message has been sent to the sender.
     *
     * This class implements Comparable interface, so that cache search, insertion,
     * and deletion could be made and optimized based on message arrival time.
     *
     * NOTE: that the class violates equals method convention:
     * a.compareTo(b) doesn't imply a.equals(b). This is done intentionally, so
     * that message search could be made straightforwardly to the programmer.
     */
    private static class CacheEntry implements Comparable<CacheEntry> {

        private final long arrivalTime;
        private final Message message;

        private CacheEntry(long arrivalTime, Message message) {
            this.arrivalTime = arrivalTime;
            this.message = message;
        }


        /**
         * The less the arrival time is, the longer the entry is in the cache.
         * All elements are meant to be sorted from oldest to earliest.
         */
        @Override
        public int compareTo(CacheEntry cacheEntry) {
            if (arrivalTime < cacheEntry.arrivalTime) return -1;
            else if (arrivalTime > cacheEntry.arrivalTime) return 1;
            else return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheEntry that = (CacheEntry) o;

            return message.equals(that.message);
        }

        @Override
        public int hashCode() {
            return message.hashCode();
        }
    }

}
