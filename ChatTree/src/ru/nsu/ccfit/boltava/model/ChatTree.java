package ru.nsu.ccfit.boltava.model;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.*;


public class ChatTree {

    private static final int REQUIRED_ARGUMENTS_COUNT = 3;
    private static final int TOTAL_ARGUMENTS_COUNT = 5;
    private static final String DEFAULT_MESSAGE_ENCODING = "utf-8";
    /**
     * @param args:
     *            1. Client name
     *            2. Port number
     *            3. Percentage of packages lost
     *            4. Parent ip address (optional)
     *
     */
    public static void main(String[] args) {
        if (args.length < REQUIRED_ARGUMENTS_COUNT) {
            printHelp();
            return;
        } else if (args.length > REQUIRED_ARGUMENTS_COUNT && args.length < TOTAL_ARGUMENTS_COUNT) {
            System.out.println("Optional arguments must all be present");
            printHelp();
            return;
        }

        try {
            String nodeName = args[0];
            Integer portNumber = Integer.valueOf(args[1]);
            if (portNumber <= 0) throw new NumberFormatException("Port number must be a positive integer");
            Integer packetLossPercentage = Integer.valueOf(args[2]);
            if (args.length == TOTAL_ARGUMENTS_COUNT) {
                String parentIpString = args[3];
                Integer parentPort = Integer.valueOf(args[4]);
                InetSocketAddress parentAddress = new InetSocketAddress(
                        InetAddress.getByName(parentIpString), parentPort
                );
                new TreeNode(
                        nodeName,
                        portNumber,
                        packetLossPercentage,
                        parentAddress
                );

            } else {
                new TreeNode(nodeName, portNumber, packetLossPercentage);
            }
        } catch (NumberFormatException e) {
            System.err.println("Failed to parse port. " + e.getMessage());
        } catch (JAXBException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("Usage: <ProgramName> NodeName PortNumber " +
                "PacketLossFactor [ParentIp] [ParentPort]");
    }


}
