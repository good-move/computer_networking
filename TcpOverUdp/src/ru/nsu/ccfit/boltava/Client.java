package ru.nsu.ccfit.boltava;

import ru.nsu.ccfit.boltava.socket.TouServerSocket;
import ru.nsu.ccfit.boltava.socket.TouSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {

    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) throws IOException {
        TouSocket socket = new TouSocket(InetAddress.getByName("localhost"), SERVER_PORT);

    }

}
