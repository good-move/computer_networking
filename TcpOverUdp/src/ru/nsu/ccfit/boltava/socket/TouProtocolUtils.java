package ru.nsu.ccfit.boltava.socket;


/**
 * Protocol segment structure:
 *
 * 0-------------31|---------------------63|
 * ========================================
 * Sequence number | Acknowledgement number|
 * ========================================
 * S |A |F | Offset | Data.................|
 * ========================================
 * 64|65|66|67----69|70--------------------|
 *
 */
public class TouProtocolUtils {

    public static int SEGMENT_HEADER_LENGTH = 9;

    private static final int FLAGS_BYTE_INDEX = 8;

    public static void setSynFlag(byte[] segmentHeader) {
        segmentHeader[FLAGS_BYTE_INDEX] |= 0b1000_0000;
    }

    public static boolean hasSynFlag(byte[] segmentHeader) {
        return ((segmentHeader[FLAGS_BYTE_INDEX] & 0b1000_0000) >> 7) == 1;
    }

    public static void setAckFlag(byte[] segmentHeader) {
        segmentHeader[FLAGS_BYTE_INDEX] |= 0b0100_0000;
    }

    public static boolean hasAckFlag(byte[] segmentHeader) {
        return ((segmentHeader[FLAGS_BYTE_INDEX] & 0b0100_0000) >> 6) == 1;
    }

    public static void setFinFlag(byte[] segmentHeader) {
        segmentHeader[FLAGS_BYTE_INDEX] |= 0b0010_0000;
    }

    public static boolean hasFinFlag(byte[] segmentHeader) {
        return ((segmentHeader[FLAGS_BYTE_INDEX] & 0b0010_0000) >> 5) == 1;
    }

    public static void writeSequenceNumber(byte[] segmentHeader, int seqNumber) {
        for (int i = 3; i >= 0; --i) {
            segmentHeader[3-i] |= (seqNumber >> (8*i)) & 0xFF;
        }
    }

    public static int readSequenceNumber(final byte[] segmentHeader) {
        int sequenceNumber = 0;
        for (int i = 3; i >= 0; --i) {
            sequenceNumber |= segmentHeader[i] << (8*i);
        }

        return sequenceNumber;
    }

    public static void writeAckNumber(byte[] segmentHeader, int ackNumber) {
        for (int i = 3; i >= 0; --i) {
            segmentHeader[7-i] |= (ackNumber >> (8*i)) & 0xFF;
        }
    }

    public static int readAckNumber(final byte[] segmentHeader) {
        int ackNumber = 0;
        for (int i = 3; i >= 0; --i) {
            ackNumber |= segmentHeader[i+4] << (8*i);
        }

        return ackNumber;
    }

}
