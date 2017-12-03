package ru.nsu.ccfit.boltava.socket.segment;

import ru.nsu.ccfit.boltava.socket.TouProtocolUtils;

import java.net.InetSocketAddress;

public class TouSynSegment extends TouSegment {

    private static byte[] content = new byte[TouProtocolUtils.SEGMENT_HEADER_LENGTH];
    static {
        TouProtocolUtils.setSynFlag(content);
    }

    public TouSynSegment(InetSocketAddress address) {
        super(address, content, content.length);
    }

}
