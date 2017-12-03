package ru.nsu.ccfit.boltava.socket.segment;

import ru.nsu.ccfit.boltava.socket.TouProtocolUtils;

import java.net.InetSocketAddress;

public class TouSynAckSegment extends TouSegment {

    public TouSynAckSegment(InetSocketAddress address) {
        super(address);
        this.changeAckFlag(true);
        this.changeSynFlag(true);
    }

    public TouSynAckSegment(InetSocketAddress address, int sequenceNumber, int ackNumber) {
        super(address, sequenceNumber, ackNumber);
        this.changeSynFlag(true);
        this.changeAckFlag(true);
    }

}
