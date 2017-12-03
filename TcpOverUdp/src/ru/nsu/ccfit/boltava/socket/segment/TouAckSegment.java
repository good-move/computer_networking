package ru.nsu.ccfit.boltava.socket.segment;

import ru.nsu.ccfit.boltava.socket.TouProtocolUtils;

import java.net.InetSocketAddress;

public class TouAckSegment extends TouSegment {

    public TouAckSegment(InetSocketAddress address) {
        super(address);
        this.changeAckFlag(true);
    }

    public TouAckSegment(InetSocketAddress address, int sequenceNumber, int ackNumber) {
        super(address, sequenceNumber, ackNumber);
        this.changeAckFlag(true);
    }

}
