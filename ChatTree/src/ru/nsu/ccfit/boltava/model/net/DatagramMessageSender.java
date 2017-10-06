package ru.nsu.ccfit.boltava.model.net;

import ru.nsu.ccfit.boltava.model.message.Message;
import ru.nsu.ccfit.boltava.model.serializer.IMessageSerializer;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class DatagramMessageSender {

    private DatagramSocket mSocket;
    private IMessageSerializer<String> mSerializer;

    public DatagramMessageSender(DatagramSocket socket, IMessageSerializer<String> serializer) throws IOException, JAXBException {
        if (socket == null) throw new IllegalArgumentException("Socket can't be null");
        if (serializer == null) throw new IllegalArgumentException("Serializer can't be null");
        mSerializer = serializer;
        mSocket = socket;
    }

    public void send(Message message, InetSocketAddress receiver) throws JAXBException, IOException {
//        System.out.println("Serializing before send....");
        String xmlString = mSerializer.serialize(message);
//        System.out.println(xmlString);
        byte[] xmlMessageBytes = xmlString.getBytes(Charset.forName("utf-8"));
        DatagramPacket packet = new DatagramPacket(xmlMessageBytes, xmlMessageBytes.length);
        packet.setAddress(receiver.getAddress());
        packet.setPort(receiver.getPort());
        mSocket.send(packet);
    }

}
