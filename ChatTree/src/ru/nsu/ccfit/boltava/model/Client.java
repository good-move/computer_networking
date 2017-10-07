package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.message.JoinMessage;
import ru.nsu.ccfit.boltava.model.message.Message;
import ru.nsu.ccfit.boltava.model.message.TextMessage;
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
import java.util.concurrent.ConcurrentHashMap;

public final class Client implements IMessageListener {

    private State state;
    private Node node;
    private final DatagramSocket socket;
    private final int packetLossFactor;
    private HashSet<IMessageRenderer> renderers = new HashSet<>();

    private Thread messageListener;


    public Client(String nodeName, int port, int packetLossFactor) throws IOException, JAXBException {
        this(nodeName, port, packetLossFactor, null);
    }

    public Client(String nodeName, int port, int packetLossFactor, InetSocketAddress parentAddress)
            throws IOException, JAXBException {
        System.out.println("Initializing node...");

        this.socket = new DatagramSocket(port);
        Neighbor parent = (parentAddress == null) ? null : new Neighbor(this.socket, parentAddress);
        this.node = new Node(nodeName, parent);
        this.packetLossFactor = packetLossFactor;
        messageListener = new Thread(new Client.MessageListener(socket, new MessageHandler(this)));
        messageListener.start();
        if (parent != null) {
            state = State.Joining;
            joinParent();
        } else {
            state = State.Running;
        }
    }


//    ***************************** Public methods *****************************

    public synchronized void addMessageRenderer(IMessageRenderer renderer) {
        renderers.add(renderer);
    }

    public synchronized void removeMessageRenderer(IMessageRenderer renderer) {
        renderers.remove(renderer);
    }

    private void notifyRenderers(TextMessage message) {
        renderers.forEach(r -> r.render(message));
    }

    public Node getNode() {
        return node;
    }


    // ***************************** Message operation routines *****************************

    /**
     * Sends message to each neighbor except the message sender.
     *
     * @param message
     * @throws InterruptedException
     */
    public void broadcastMessage(Message message) throws InterruptedException {
        for (Neighbor neighbor : node.getNeighbors()) {
            if (!neighbor.getAddress().equals(message.getSender())) {
                neighbor.feedMessage(message);
            }
        }
    }

    /**
     * Sends message to the specified neighbor address. If no neighbor with such address
     * exists, nothing happens.
     *
     * @param message - message to send
     * @param receiver - address of the neighbor - messages receiver
     * @throws InterruptedException
     */
    public void sendTo(Message message, InetSocketAddress receiver) throws InterruptedException {
        Neighbor neighbor = node.getChild(receiver);
        if (neighbor != null) {
            neighbor.feedMessage(message);
        } else {

        }
    }

    @Override
    public void onTextMessageEntered(String message) {
        try {
            if (this.state == State.Running) {
                broadcastMessage(new TextMessage(message, node.getNodeName()));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("Client --- Failed to send text message: " + message);
        }
    }

//    ***************************** Internal state manipulation routines *****************************

    private void joinParent() {
        try {
            node.getParent().feedMessage(new JoinMessage(node.getNodeName()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * MessageListener is supposed to wait for a udp packet to arrive,
     * register it as "received" and delegate it to an IMessageHandler
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

        MessageListener(DatagramSocket socket, MessageHandler handler) throws JAXBException {
            this.handler = handler;
            receiver = new DatagramMessageReceiver(socket, new XmlMessageSerializer());
        }

        @Override
        public void run() {
            try {
                DatagramMessageReceiver.MessageWrapper wrapper = receiver.receive();
                Message message = wrapper.getMessage();
                message.setSender(new InetSocketAddress(wrapper.getAddress(), wrapper.getPort()));
                message.handle(handler);
            } catch (IOException | JAXBException e) {
                e.printStackTrace();
            }
        }

    }

    public enum State {
        Joining,
        Running,
        Shutting,
        Moving,
        Terminated
    }

    public final class Node {

        private final String nodeName;
        private final Neighbor parent;
        private ConcurrentHashMap<InetSocketAddress, Neighbor> children = new ConcurrentHashMap<>();

        Node(String name, Neighbor parent) {
            this.nodeName = name;
            this.parent = parent;
        }

        public String getNodeName() {
            return nodeName;
        }

        /**
         * Adds child to children list. If such child already exists, nothing happens.
         * @param address - child to add
         * @throws IOException
         * @throws JAXBException
         */
        public void addChild(InetSocketAddress address) throws IOException, JAXBException {
            children.put(address, new Neighbor(socket, address));
        }

        /**
         * Removes specified child from children list. If such child doesn't exist,
         * nothing happens
         * @param address - child to remove
         */
        public void removeChild(InetSocketAddress address) {
            children.remove(address);
        }

        public Neighbor getChild(InetSocketAddress address) {
            return children.get(address);
        }

        public Neighbor getParent() {
            return parent;
        }

        public ArrayList<Neighbor> getChildren() {
            return new ArrayList<>(children.values());
        }

        public ArrayList<Neighbor> getNeighbors() {
            ArrayList<Neighbor> neighbors = getChildren();
            neighbors.add(parent);
            return neighbors;
        }

    }

}
