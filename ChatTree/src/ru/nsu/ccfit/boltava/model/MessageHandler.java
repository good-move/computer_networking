package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.event.AckReceivedEvent;
import ru.nsu.ccfit.boltava.model.event.EventDispatcher;
import ru.nsu.ccfit.boltava.model.message.*;
import static ru.nsu.ccfit.boltava.model.Client.State.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.InetSocketAddress;

public class MessageHandler implements IMessageHandler {

    private final Client client;
    private EventDispatcher<AckReceivedEvent> eventDispatcher;

    MessageHandler(Client client, EventDispatcher<AckReceivedEvent> dispatcher) {
        this.eventDispatcher = dispatcher;
        this.client = client;
    }

    @Override
    public void handle(AckMessage message) {
//        System.out.println("Received ACK message");
        // should publish ACK event
        eventDispatcher.publish(new AckReceivedEvent(message.getSender(), message.getId()));
    }

    @Override
    public void handle(ErrorMessage message) {
        // log error to stderr
    }

    @Override
    public void handle(JoinMessage message) {
        if ((client.getState() == RUNNING || client.getState() == MOVING) &&
            !client.isMessageRegistered(message)) {
            System.out.println("Received new JOIN message");
            try {
                client.registerMessage(message);
                System.out.println(message.getNodeName() + " has joined chat");
                client.getNode().addChild(message.getSender());
                client.sendTo(new AckMessage(message.getId()), message.getSender());
            } catch (IOException | JAXBException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(LeaveMessage message) {

        if ((client.getState() == RUNNING || client.getState() == MOVING) &&
            client.getNode().getChild(message.getSender()) != null) {
            System.out.println("Received LEAVE message");
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
        if(client.isRoot()) return;

        InetSocketAddress parentAddress = client.getNode().getParent().getAddress();
        if (client.getState() == RUNNING &&
            message.getSender().equals(parentAddress) &&
            !client.isMessageRegistered(message)) {
            System.out.println("Received REJOIN message");

            try {
                client.registerMessage(message);
                client.setState(MOVING);
                // in order to be able to send message to next parent
                client.getNode().addChild(message.getNewParentAddress());

                client.sendTo(
                    new JoinMessage(client.getNode().getName()),
                    message.getNewParentAddress(),
                    () -> {
                        try {
                            client.sendTo(new AckMessage(message.getId()), parentAddress);
                            client.detachParent();
                            // remove temporary child and set new parent
                            client.getNode().removeChild(message.getNewParentAddress());
                            client.setParent(message.getNewParentAddress());

                            client.setState(RUNNING);
                        } catch (InterruptedException | JAXBException | IOException e) {
                            e.printStackTrace();
                            System.err.println("Failed to set new parent");
                        }
                    },() -> {
                            client.detachParent();
                            client.getNode().removeChild(message.getNewParentAddress());
                            client.setState(RUNNING);
                        }
                );
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (JAXBException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(RootMessage message) {
        if (client.getState() == RUNNING &&
            !client.isRoot() &&
            message.getSender().equals(client.getNode().getParent().getAddress()) &&
            !client.isMessageRegistered(message)) {
            System.out.println("Received ROOT message");
            try {
                client.registerMessage(message);
                client.sendTo(new AckMessage(message.getId()), message.getSender());
                client.detachParent();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(TextMessage message) {
        if ((client.getState() == RUNNING || client.getState() == MOVING) &&
            !client.isMessageRegistered(message)) {
            System.out.println("Received TEXT message");
            try {
                client.registerMessage(message);
                System.out.println("Sending AckText to " + message.getSenderName());
                client.sendTo(
                    new AckMessage(message.getId()), message.getSender(),
                    null,
                    () -> System.out.println("Sent AckText to " + message.getSenderName())
                );
                client.showMessage(message);
                client.broadcastMessage(message);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
