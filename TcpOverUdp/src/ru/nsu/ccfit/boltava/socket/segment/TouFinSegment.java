package ru.nsu.ccfit.boltava.socket.segment;

import ru.nsu.ccfit.boltava.socket.TouProtocolUtils;

import java.net.InetSocketAddress;

public class TouFinSegment extends TouSegment {

    private static byte[] content = new byte[TouProtocolUtils.SEGMENT_HEADER_LENGTH];
    static {
        TouProtocolUtils.setFinFlag(content);
    }

    public TouFinSegment(InetSocketAddress address) {
        super(address, content, content.length);
    }

}
