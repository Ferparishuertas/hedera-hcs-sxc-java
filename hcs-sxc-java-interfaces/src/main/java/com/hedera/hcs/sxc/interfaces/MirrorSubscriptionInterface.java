package com.hedera.hcs.sxc.interfaces;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.hedera.hashgraph.sdk.consensus.ConsensusTopicId;

public interface MirrorSubscriptionInterface {
    public void init(HCSCallBackFromMirror onHCSMessageCallback, long applicationId, Optional<Instant> lastConsensusTimestamp, String mirrorAddress, List<ConsensusTopicId> topicIds) throws Exception;    
}