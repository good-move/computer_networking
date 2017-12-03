package ru.nsu.ccfit.boltava.socket;

import ru.nsu.ccfit.boltava.socket.segment.TouSegment;

public interface ISegmentsHandler {

    void onAckReceived(TouSegment segment);
    void onSynReceived(TouSegment segment);
    void onSynAckReceived(TouSegment segment);
    void onFinReceived(TouSegment segment);

}
