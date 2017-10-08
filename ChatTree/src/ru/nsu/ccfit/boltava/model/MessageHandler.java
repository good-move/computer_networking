package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.message.*;
import static ru.nsu.ccfit.boltava.model.Client.State.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.InetSocketAddress;

public class MessageHandler implements IMessageHandler {

    private final Client client;

    MessageHandler(Client client) {
        this.client = client;
    }

    @Override
    public void handle(AckMessage message) {
        System.out.println("Received ACK message");
        // should publish ACK event
    }

    @Override
    public void handle(ErrorMessage message) {
        // log error to stderr
    }

    @Override
    public void handle(JoinMessage message) {
        System.out.println("Received JOIN message");
        if ((client.getState() == Running || client.getState() == Moving) &&
            !client.isMessageRegistered(message)) {
            try {
                client.registerMessage(message);
                System.out.println(message.getNodeName() + " has joined chat");
                client.getNode().addChild(message.getSender());
                System.out.println("Sending AckJoin to " + message.getNodeName());
                client.sendTo(new AckMessage(message.getId()), message.getSender());
            } catch (IOException | JAXBException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(LeaveMessage message) {
        System.out.println("Received LEAVE message");

        if ((client.getState() == Running || client.getState() == Moving) &&
            client.getNode().getChild(message.getSender()) != null) {
            try {
                client.getNode().removeChild(message.getSender());
                client.sendTo(new AckMessage(message.getId()), message.getSender());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(RejoinMessage message) {
        System.out.println("Received REJOIN message");
        if(client.isRoot()) return;

        InetSocketAddress parentAddress = client.getNode().getParent().getAddress();
        if (client.getState() == Running &&
            message.getSender().equals(parentAddress)) {
            try {
                client.setState(Moving);
                client.sendTo(
                        new JoinMessage(client.getNode().getName()),
                        message.getNewParentAddress(),
                        () -> {
                            try {
                                client.sendTo(new AckMessage(message.getId()), parentAddress);
                                client.detachParent();
                                client.setParent(message.getNewParentAddress());
                                client.setState(Running);
                            } catch (InterruptedException | JAXBException | IOException e) {
                                e.printStackTrace();
                                System.err.println("Failed to set new parent");
                            }
                        },
                        client::detachParent
                );
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(RootMessage message) {
        System.out.println("Received ROOT message");

        if (client.getState() == Running &&
            !client.isRoot() &&
            message.getSender().equals(client.getNode().getParent().getAddress())) {
            try {
                client.sendTo(new AckMessage(message.getId()), message.getSender());
                client.detachParent();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(TextMessage message) {
        System.out.println("Received TEXT message");

        if (client.getState() == Running || client.getState() == Moving) {
            try {
                System.out.println("Sending AckText to " + message.getSenderName());
                client.sendTo(new AckMessage(message.getId()), message.getSender());
                System.out.println("Sent AckText to " + message.getSenderName());
                if (!client.isMessageRegistered(message)) {
                    client.registerMessage(message);
                    client.showMessage(message);
                    client.broadcastMessage(message);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
