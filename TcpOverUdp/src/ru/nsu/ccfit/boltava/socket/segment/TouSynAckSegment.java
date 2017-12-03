package ru.nsu.ccfit.boltava.socket.segment;

import ru.nsu.ccfit.boltava.socket.TouProtocolUtils;

import java.net.InetSocketAddress;

public class TouSynAckSegment extends TouSegment {

    private static byte[] header = new byte[TouProtocolUtils.SEGMENT_HEADER_LENGTH];
    static {
        TouProtocolUtils.setSynFlag(header);
        TouProtocolUtils.setAckFlag(header);
    }

    public TouSynAckSegment(InetSocketAddress address) {
        super(address, header, header.length);
    }

}
