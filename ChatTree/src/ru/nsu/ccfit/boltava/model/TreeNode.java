package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.message.JoinMessage;
import ru.nsu.ccfit.boltava.model.message.Message;
import ru.nsu.ccfit.boltava.model.message.TextMessage;
import ru.nsu.ccfit.boltava.model.net.DatagramMessageSender;
import ru.nsu.ccfit.boltava.model.serializer.XmlMessageSerializer;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.*;


/**
 * Class which implements the Chat Tree Client abstraction.
 *
 * Client has three states:
 *  - Joining (Client tries to connect to the parent, if one's been supplied,
 *  and waits for ACK serializer, ignoring all other messages.)
 *  - Running (Listens to all messages and enables console input to enter text messages,
 *  which will be sent to all other nodes.)
 *  - Shutting (Client wants to shut down and is in the process of tree
 *  restructuring: Client's neighbors have to be rejoined before this Client can shut down completely.
 *  All chat functions are preserved.)
 *  - Terminated (Enters this state only after Shutting state. If this state
 *  is active, tree restructuring has completed successfully and Client can shut down freely.
 *  All chat functions are disabled.)
 *
 * It listens to all incoming messages and passes them to MessageHandler,
 * which can modify Client's state through public setState() method.
 *
 *
 *
 */
public class TreeNode {

    private static final String DEFAULT_MESSAGE_ENCODING = "utf-8";
    private static final int REGISTERED_MSG_BUFFER_SIZE = 100;

    private ExecutorService mExecutor = Executors.newFixedThreadPool(20);

    private ConcurrentHashMap<UUID, Message> mPendingMessages = new ConcurrentHashMap<>();
    private ConcurrentLinkedQueue<UUID> mRegisteredMessagesQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentHashMap<UUID, UUID> mRegisteredMessagesHashMap = new ConcurrentHashMap<>();

    private DatagramSocket mSocket;

    private InetSocketAddress mParent;
    private InetSocketAddress mNextParent;
    private ConcurrentHashMap<InetSocketAddress, InetSocketAddress> mChildren = new ConcurrentHashMap<>();
    private boolean mIsRoot = false;
    private String mNodeName;

    private State mState;
    private int mPacketLossFactor;

    private Thread mMessageListener;
    private Thread mMessageReader;
    private DatagramMessageSender mMessageSender;

    private final Object lock = new Object();


    public TreeNode(String nodeName, int port, int packetLossFactor) throws IOException, JAXBException {
        init(nodeName, port, packetLossFactor);
        mIsRoot = true;
        setState(State.Running);
        launchChat();
    }

    public TreeNode(String nodeName, int port, int packetLossFactor, InetSocketAddress parentAddress)
            throws IOException, JAXBException {
        init(nodeName, port, packetLossFactor);
        mIsRoot = false;
        mParent = parentAddress;
        setState(State.Starting);
        System.out.println("Trying to join chat tree...");
//        CompletableFuture.runAsync(() -> sendMessageRobust(new JoinMessage(mNodeName), mParent))
//                .thenRun(() -> System.out.println("Joined chat tree"));
        sendMessageRobust(new JoinMessage(mNodeName), mParent);
        System.out.println("Joined chat tree");
    }

    private void init(String nodeName, int port, int packetLossFactor) throws IOException, JAXBException {
        System.out.println("Initializing node...");
        mNodeName = nodeName;
        mSocket = new DatagramSocket(port);
        mPacketLossFactor = packetLossFactor;
        mMessageListener = new Thread(new MessageListener(mSocket, new MessageHandler(this)));
        mMessageListener.start();
        mMessageSender = new DatagramMessageSender(mSocket, new XmlMessageSerializer());
    }

    public boolean isRoot() {
        return mIsRoot;
    }

    synchronized void setState(State nextState) {
        mState = nextState;
        System.out.println("Changed state to " + mState.toString());
    }

    synchronized State getState() {
        return mState;
    }

    void sendMessage(Message message, InetSocketAddress receiver) throws JAXBException, IOException {
        mMessageSender.send(message, receiver);
    }

    private void sendMessageRobust(Message message, InetSocketAddress receiver) throws JAXBException, IOException {
        addPending(message);
        while (isPending(message.getId())) {
            try {
                sendMessage(message, receiver);
//                lock.wait(500);
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void showMessage(TextMessage message) {
        System.out.println(message.getSenderName() + ": " + message.getText());
    }

    private void addPending(Message message) {
        mPendingMessages.put(message.getId(), message);
    }

    void removePending(Message message) {
        mPendingMessages.remove(message.getId());
    }

    boolean isPending(UUID uuid) {
        return mPendingMessages.containsKey(uuid);
    }

    Message getPending(UUID uuid) {
        return mPendingMessages.get(uuid);
    }

    void addChild(InetSocketAddress childAddress) {
        mChildren.put(childAddress, childAddress);
    }

    void removeChild(InetSocketAddress childAddress) {
        mChildren.remove(childAddress);
    }

    void broadсastMessage(Message message, InetSocketAddress exceptionAddress) {
        for (InetSocketAddress child : mChildren.keySet()) {
            if (!child.equals(exceptionAddress)) {
                mExecutor.submit(() -> {
                    try {
                        sendMessageRobust(message, mParent);
                    } catch (JAXBException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }

        if (!mParent.equals(exceptionAddress)) {
            mExecutor.submit(() -> {
                try {
                    sendMessageRobust(message, mParent);
                } catch (JAXBException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

    }

    private void broadсastMessage(Message message) {
        for (InetSocketAddress child : mChildren.keySet()) {
            mExecutor.submit(() -> {
                try {
                    sendMessageRobust(message, child);
                } catch (JAXBException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
        mExecutor.submit(() -> {
            try {
                sendMessageRobust(message, mParent);
            } catch (JAXBException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    void launchChat() {
        mMessageReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while (!Thread.interrupted() && (line = reader.readLine()) != null) {
                    broadсastMessage(new TextMessage(line.trim(), mNodeName));
                }
            } catch (IOException e) {
                System.out.println("Console Reader is broken!");
                e.printStackTrace();
            }

        });
        mMessageReader.start();
    }

    void confirmNextParent() {
        if (mNextParent == null) {
            throw new RuntimeException("Cannot set new parent to be null");
        }
        mParent = mNextParent;
        mNextParent = null;
    }

    InetSocketAddress getParent() {
        return mParent;
    }

    void setNextParent(InetSocketAddress nextParent) {
        mNextParent = nextParent;
    }

    InetSocketAddress getNextParent() {
        return mNextParent;
    }

    void joinNextParent() {

    }

    synchronized void register(Message message) {
        UUID messageId = message.getId();
        if (!mRegisteredMessagesHashMap.containsKey(messageId)) {
            if (mRegisteredMessagesQueue.size() >= REGISTERED_MSG_BUFFER_SIZE) {
                UUID id = mRegisteredMessagesQueue.poll();
                mRegisteredMessagesHashMap.remove(id);
            }
            mRegisteredMessagesQueue.add(messageId);
            mRegisteredMessagesHashMap.put(messageId, messageId);
        }
    }

    boolean isRegistered(Message message) {
        return mRegisteredMessagesHashMap.containsKey(message.getId());
    }

    public enum State {
        Starting,
        Running,
        Shutting,
        Moving,
        Terminated
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
    private class MessageListener implements Runnable {

        private static final int MAX_PACKET_SIZE = 64 * 1024;
        private DatagramSocket mSocket;
        private XmlMessageSerializer mSerializer = new XmlMessageSerializer();
        private IMessageHandler mMessageHandler;

        MessageListener(DatagramSocket socket, IMessageHandler handler) throws IOException, JAXBException {
            mSocket = socket;
            mMessageHandler = handler;
        }


        /**
         * receive packet
         * imitate packet loss
         * pass it over to handler
         */
        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    byte[] bytes = new byte[MAX_PACKET_SIZE];
                    DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                    mSocket.receive(packet);
                    if (!isPacketLost()) {
                        System.out.println("\nNEW MESSAGE!!!\n");
                        mExecutor.submit(() -> {
                            try {
                                String xmlMessage = new String(
                                        packet.getData(),
                                        0,
                                        packet.getLength(),
                                        Charset.forName(DEFAULT_MESSAGE_ENCODING));
                                Message message = mSerializer.deserialize(xmlMessage);
                                message.setSender(new InetSocketAddress(packet.getAddress(), packet.getPort()));
                                message.handle(mMessageHandler);
                            } catch (JAXBException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            } catch (IOException e) {
                System.out.println("Message Listener is broken!");
                e.printStackTrace();
            }
        }

        private boolean isPacketLost() {
            return false;
//            return new Random().nextInt() % 100 <= mPacketLossFactor;
        }

    }

}
