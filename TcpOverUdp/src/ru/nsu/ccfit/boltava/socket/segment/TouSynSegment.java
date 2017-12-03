package ru.nsu.ccfit.boltava.socket.segment;

import ru.nsu.ccfit.boltava.socket.TouProtocolUtils;

import java.net.InetSocketAddress;

public class TouSynSegment extends TouSegment {

    public TouSynSegment(InetSocketAddress address) {
        super(address);
        changeSynFlag(true);
    }

    public TouSynSegment(InetSocketAddress address, int sequenceNumber, int ackNumber) {
        super(address, sequenceNumber, ackNumber);
        this.changeSynFlag(true);
    }

}
