package ru.nsu.ccfit.boltava;

import ru.nsu.ccfit.boltava.message.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;

import static ru.nsu.ccfit.boltava.TreeNode.State.*;

public class MessageHandler implements IMessageHandler {

    private final TreeNode mTreeNode;

    MessageHandler(TreeNode treeNode) {
        mTreeNode = treeNode;
    }

    @Override
    public void handle(AckMessage message) {
        System.out.println("Received ACK message");

        //if no matching pair found, ignore message
        if (!mTreeNode.isPending(message.getId())) {
            try {
                mTreeNode.sendMessage(new ErrorMessage(/*errText, errCode*/), message.getSender());
            } catch (JAXBException | IOException e) {
                e.printStackTrace();
            }
        }


        Message sentMessage = mTreeNode.getPending(message.getId());
        Class sentMessageClass = sentMessage.getClass();

        switch (mTreeNode.getState()) {
            case Starting: {
                if (sentMessageClass.equals(JoinMessage.class)) {
                    mTreeNode.removePending(sentMessage);
                    mTreeNode.launchChat();
                    mTreeNode.setState(Running);
                }
            } break;
            case Running: {
                if (sentMessageClass.equals(TextMessage.class)) {
                    mTreeNode.removePending(sentMessage);
                }
            } break;

            case Moving: {
                if (sentMessageClass.equals(JoinMessage.class)) {
                    mTreeNode.removePending(sentMessage);
//                    mTreeNode.sendMessage(new AckMessage(/* ID OF REJOIN MESSAGE */), mTreeNode.getParent());
                    mTreeNode.confirmNextParent();
                    mTreeNode.setState(Running);
                }
            } break;

            case Shutting: {
                if (sentMessageClass.equals(RejoinMessage.class)) {
                    mTreeNode.removePending(sentMessage);
                    try {
                        mTreeNode.sendMessage(new AckMessage(sentMessage.getId()), sentMessage.getSender());
                    } catch (JAXBException | IOException e) {
                        e.printStackTrace();
                    }
                } else if (sentMessageClass.equals(RootMessage.class)) {

                } else if (sentMessageClass.equals(LeaveMessage.class)) {
                    mTreeNode.setState(Terminated);
                }
            } break;
        }
    }

    @Override
    public void handle(ErrorMessage message) {
        // log message to stderr
    }

    @Override
    public void handle(JoinMessage message) {
        System.out.println("Received JOIN message");

        if (mTreeNode.getState() == Running || mTreeNode.getState() == Moving) {
            if (!mTreeNode.isRegistered(message)) {
                mTreeNode.register(message);
                System.out.println(message.getNodeName() + " has joined chat");
                mTreeNode.addChild(message.getSender());
            }
            System.out.println("Sending AckJoin to " + message.getNodeName());
            try {
                mTreeNode.sendMessage(new AckMessage(message.getId()), message.getSender());
            } catch (JAXBException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(LeaveMessage message) {
        System.out.println("Received LEAVE message");

        TreeNode.State state = mTreeNode.getState();
        if (state == Running || state == Moving) {
            mTreeNode.removeChild(message.getSender());
            try {
                mTreeNode.sendMessage(new AckMessage(message.getId()), message.getSender());
            } catch (JAXBException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(RejoinMessage message) {
        System.out.println("Received REJOIN message");

        if (mTreeNode.getState() == Running &&
            message.getSender().equals(mTreeNode.getParent())) {
            mTreeNode.setState(Moving);
            mTreeNode.setNextParent(message.getNextParent());
            mTreeNode.joinNextParent();
        }
    }

    @Override
    public void handle(RootMessage message) {
        System.out.println("Received ROOT message");

    }

    @Override
    public void handle(TextMessage message) {
        System.out.println("Received TEXT message");

        if (mTreeNode.getState() == Running || mTreeNode.getState() == Moving) {
            System.out.println("Sending AckText to " + message.getSenderName());
            try {
                mTreeNode.sendMessage(new AckMessage(message.getId()), message.getSender());
            } catch (JAXBException | IOException e) {
                e.printStackTrace();
            }
            System.out.println("Sent AckText to " + message.getSenderName());
            if (!mTreeNode.isRegistered(message)) {
                mTreeNode.register(message);
                mTreeNode.showMessage(message);
                mTreeNode.broadсastMessage(message, message.getSender());
            }
        }
    }

}
