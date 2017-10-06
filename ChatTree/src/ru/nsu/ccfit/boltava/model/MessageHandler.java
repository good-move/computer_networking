package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.message.*;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public class MessageHandler implements IMessageHandler {

    private final TreeNode mTreeNode;

    MessageHandler(TreeNode treeNode) {
        mTreeNode = treeNode;
    }

    @Override
    public void handle(AckMessage message) {
        System.out.println("Received ACK serializer");

        //if no matching pair found, ignore serializer
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
                    mTreeNode.setState(TreeNode.State.Running);
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
                    mTreeNode.setState(TreeNode.State.Running);
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
                    mTreeNode.setState(TreeNode.State.Terminated);
                }
            } break;
        }
    }

    @Override
    public void handle(ErrorMessage message) {
        // log serializer to stderr
    }

    @Override
    public void handle(JoinMessage message) {
        System.out.println("Received JOIN serializer");

        if (mTreeNode.getState() == TreeNode.State.Running || mTreeNode.getState() == TreeNode.State.Moving) {
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
        System.out.println("Received LEAVE serializer");

        TreeNode.State state = mTreeNode.getState();
        if (state == TreeNode.State.Running || state == TreeNode.State.Moving) {
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
        System.out.println("Received REJOIN serializer");

        if (mTreeNode.getState() == TreeNode.State.Running &&
            message.getSender().equals(mTreeNode.getParent())) {
            mTreeNode.setState(TreeNode.State.Moving);
//            mTreeNode.setNextParent(message.getNextParent());
            mTreeNode.joinNextParent();
        }
    }

    @Override
    public void handle(RootMessage message) {
        System.out.println("Received ROOT serializer");

    }

    @Override
    public void handle(TextMessage message) {
        System.out.println("Received TEXT serializer");

        if (mTreeNode.getState() == TreeNode.State.Running || mTreeNode.getState() == TreeNode.State.Moving) {
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
