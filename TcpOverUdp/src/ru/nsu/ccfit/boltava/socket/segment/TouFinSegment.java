package ru.nsu.ccfit.boltava.socket.segment;

import ru.nsu.ccfit.boltava.socket.TouProtocolUtils;

import java.net.InetSocketAddress;

public class TouFinSegment extends TouSegment {

    public TouFinSegment(InetSocketAddress address) {
        super(address);
        this.changeFinFlag(true);
    }

    public TouFinSegment(InetSocketAddress address, int sequenceNumber, int ackNumber) {
        super(address, sequenceNumber, ackNumber);
        this.changeFinFlag(true);
    }

}
