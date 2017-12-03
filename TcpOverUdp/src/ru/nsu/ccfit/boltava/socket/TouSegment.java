package ru.nsu.ccfit.boltava.socket;

import java.net.InetSocketAddress;

public class TouSegment {

    private InetSocketAddress address;

    private int sequenceNumber;
    private int acknowledgementNumber;
    private boolean ackFlag;
    private boolean synFlag;
    private boolean finFlag;
    private SegmentType type;
    private byte[] payload = new byte[0];

    public TouSegment(InetSocketAddress address, byte[] segmentContent, int length) {
        this.address = address;
        parseSegmentContent(segmentContent, length);
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public int getAcknowledgementNumber() {
        return acknowledgementNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setAckNumber(int ackNumber) {
        this.acknowledgementNumber = ackNumber;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public SegmentType getType() {
        return type;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte[] toBytes() {
        byte[] segment = new byte[TouProtocolUtils.SEGMENT_HEADER_LENGTH + payload.length];
        TouProtocolUtils.writeSequenceNumber(segment, sequenceNumber);
        TouProtocolUtils.writeAckNumber(segment, acknowledgementNumber);
        if (ackFlag) {
            TouProtocolUtils.setAckFlag(segment);
        }
        if (finFlag) {
            TouProtocolUtils.hasFinFlag(segment);
        }
        if (synFlag) {
            TouProtocolUtils.hasFinFlag(segment);
        }
        // write payload into segment
        System.arraycopy(payload, 0, segment, TouProtocolUtils.SEGMENT_HEADER_LENGTH, payload.length);
        return  segment;
    }

    private void parseSegmentContent(byte[] content, int length) {
        sequenceNumber = TouProtocolUtils.readSequenceNumber(content);
        acknowledgementNumber = TouProtocolUtils.readAckNumber(content);
        payload = new byte[length - TouProtocolUtils.SEGMENT_HEADER_LENGTH];
        synFlag = TouProtocolUtils.hasSynFlag(content);
        finFlag = TouProtocolUtils.hasFinFlag(content);
        ackFlag = TouProtocolUtils.hasAckFlag(content);

        System.arraycopy(
                content,
                TouProtocolUtils.SEGMENT_HEADER_LENGTH,
                payload, 0,
                payload.length
        );

        if (TouProtocolUtils.hasFinFlag(content)) {
            type = SegmentType.FIN;
        } else if (TouProtocolUtils.hasSynFlag(content)) {
            if (TouProtocolUtils.hasAckFlag(content)) {
                type = SegmentType.SYNACK;
            } else {
                type= SegmentType.SYN;
            }
        } else {
            type = SegmentType.ACK;
        }
    }

    public void changeAckFlag(boolean flagValue) {
        ackFlag = flagValue;
    }

    public void changeFinFlag(boolean flagValue) {
        finFlag = flagValue;
    }

    public void changeSynFlag(boolean flagValue) {
        synFlag = flagValue;
    }

    public enum SegmentType {
        SYN,
        ACK,
        FIN,
        SYNACK
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TouSegment segment = (TouSegment) o;

        return sequenceNumber == segment.sequenceNumber;
    }

    @Override
    public int hashCode() {
        return sequenceNumber;
    }

}
