package ru.nsu.ccfit.boltava.net;

import ru.nsu.ccfit.boltava.message.Message;
import ru.nsu.ccfit.boltava.serializer.IMessageSerializer;
import ru.nsu.ccfit.boltava.serializer.XmlMessageSerializer;

import javax.xml.bind.JAXBException;
import javax.xml.crypto.Data;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

public class DatagramMessageSender {

    private DatagramSocket mSocket;
    private IMessageSerializer<String> mSerializer;

    public DatagramMessageSender(DatagramSocket socket, IMessageSerializer<String> serializer) throws IOException, JAXBException {
        if (socket == null) throw new IllegalArgumentException("Socket can't be null");
        mSerializer = serializer;
        mSocket = socket;
    }

    public void send(Message message, InetSocketAddress receiver) throws JAXBException, IOException {
        System.out.println("Serializing before send....");
        String xmlString = mSerializer.serialize(message);
        System.out.println(xmlString);
        byte[] xmlMessageBytes = xmlString.getBytes(Charset.forName("utf-8"));
        DatagramPacket packet = new DatagramPacket(xmlMessageBytes, xmlMessageBytes.length);
        packet.setAddress(receiver.getAddress());
        packet.setPort(receiver.getPort());
//        System.out.println(packet.getAddress().toString());
//        System.out.println(packet.getPort());
        mSocket.send(packet);
    }

}
