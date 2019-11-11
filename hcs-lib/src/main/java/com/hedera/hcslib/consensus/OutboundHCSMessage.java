package com.hedera.hcslib.consensus;

import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaException;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.TransactionId;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.account.AccountId;
import com.hedera.hashgraph.sdk.consensus.SubmitMessageTransaction;
import com.hedera.hashgraph.sdk.consensus.TopicId;
import com.hedera.hashgraph.sdk.crypto.ed25519.Ed25519PrivateKey;
import com.hedera.hcslib.HCSLib;
import com.hedera.hcslib.proto.java.AccountID;
import com.hedera.hcslib.proto.java.ApplicationMessage;
import com.hedera.hcslib.proto.java.ApplicationMessageChunk;
import com.hedera.hcslib.proto.java.Timestamp;
import com.hedera.hcslib.proto.java.TransactionID;
import java.util.Arrays;

import lombok.extern.log4j.Log4j2;

@Log4j2
public final class OutboundHCSMessage {

    private boolean signMessages = false;
    private boolean encryptMessages = false;
    private boolean rotateKeys = false;
    private int rotationFrequency = 0;
    private Map<AccountId, String> nodeMap = new HashMap<AccountId, String>();
    private AccountId operatorAccountId = new AccountId(0, 0, 0);
    private Ed25519PrivateKey ed25519PrivateKey;
    private List<TopicId> topicIds = new ArrayList<TopicId>();

    public OutboundHCSMessage(HCSLib hcsLib) {
        this.signMessages = hcsLib.getSignMessages();
        this.encryptMessages = hcsLib.getEncryptMessages();
        this.rotateKeys = hcsLib.getRotateKeys();
        this.nodeMap = hcsLib.getNodeMap();
        this.operatorAccountId = hcsLib.getOperatorAccountId();
        this.ed25519PrivateKey = hcsLib.getEd25519PrivateKey();
        this.topicIds = hcsLib.getTopicIds();
    }

    public OutboundHCSMessage overrideMessageSignature(boolean signMessages) {
        this.signMessages = signMessages;
        return this;
    }

    public OutboundHCSMessage overrideEncryptedMessages(boolean encryptMessages) {
        this.encryptMessages = encryptMessages;
        return this;
    }

    public OutboundHCSMessage overrideKeyRotation(boolean keyRotation, int frequency) {
        this.rotateKeys = keyRotation;
        this.rotationFrequency = frequency;
        return this;
    }

    public OutboundHCSMessage overrideNodeMap(Map<AccountId, String> nodeMap) {
        this.nodeMap = nodeMap;
        return this;
    }

    public OutboundHCSMessage overrideOperatorAccountId(AccountId operatorAccountId) {
        this.operatorAccountId = operatorAccountId;
        return this;
    }

    public OutboundHCSMessage overrideOperatorKey(Ed25519PrivateKey ed25519PrivateKey) {
        this.ed25519PrivateKey = ed25519PrivateKey;
        return this;
    }

    /**
     * Sends a single cleartext message
     *
     * @param topicIndex the index reference in one of {@link #topicIds}
     * @param message
     * @return
     * @throws HederaNetworkException
     * @throws IllegalArgumentException
     * @throws HederaException
     */
    public boolean sendMessage(int topicIndex, String message) throws HederaNetworkException, IllegalArgumentException, HederaException {

        if (signMessages) {

        }
        if (encryptMessages) {

        }
        if (rotateKeys) {
            int messageCount = 0; //TODO - keep track of messages app-wide, not just here. ( per topic )
            if (messageCount > rotationFrequency) {
            }
        }

        // generate TXId for main and first message
        TransactionId transactionId = new TransactionId(this.operatorAccountId);

        //break up
        List<ApplicationMessageChunk> parts = chunk(transactionId, message);
        // send each part to the network
        
        Client client = new Client(this.nodeMap);
        client.setOperator(
                this.operatorAccountId,
                 this.ed25519PrivateKey
        );
        //TODO: Make this configurable
        client.setMaxTransactionFee(100_000_000L);
        
        for (ApplicationMessageChunk messageChunk : parts) {
            TransactionReceipt receipt = new SubmitMessageTransaction(client)
                .setMessage(messageChunk.toByteArray())
                .setTopicId(this.topicIds.get(topicIndex))
                .setTransactionId(transactionId) 
                .executeForReceipt();
            log.info("status is {} "
                    + "sequence no is {}"
                    ,receipt.getStatus()
                    ,receipt.getTopicSequenceNumber()
            );
        }
        
        return true;
    }

    public static  List<ApplicationMessageChunk> chunk(TransactionId transactionId,  String message) {

        TransactionID transactionID = TransactionID.newBuilder()
                .setAccountID(AccountID.newBuilder()
                        .setShardNum(transactionId.getAccountId().getShardNum())
                        .setRealmNum(transactionId.getAccountId().getRealmNum())
                        .setAccountNum(transactionId.getAccountId().getAccountNum())
                        .build()
                )
                .setTransactionValidStart(Timestamp.newBuilder()
                        .setSeconds(transactionId.getValidStart().getEpochSecond())
                        .setNanos(transactionId.getValidStart().getNano())
                        .build()
                ).build();

        byte[] originalMessage = message.getBytes();

        ApplicationMessage applicationMessage = ApplicationMessage
                .newBuilder()
                .setApplicationMessageId(transactionID)
                .setBusinessProcessMessage(ByteString.copyFrom(originalMessage))
                .build();
        
        List<ApplicationMessageChunk> parts = new ArrayList<>();
        
        //TransactionID transactionID = messageEnvelope.getMessageEnvelopeId();
        byte[] amByteArray = applicationMessage.toByteArray();
        final int amByteArrayLength = amByteArray.length;
        // break up byte array into 3500 bytes parts
        final int chunkSize = 3500; // the hcs tx limit is 4k - there are header bytes that will be added to that
        int totalParts = (int) Math.ceil((double) amByteArrayLength / chunkSize);
        // chunk and send to network
        for (int i = 0, partId = 1; i < amByteArrayLength; i += chunkSize, partId++) {
            
            byte[] amMessageChunk = Arrays.copyOfRange(
                    amByteArray,
                    i,
                    Math.min(amByteArrayLength, i + chunkSize)
            );

            ApplicationMessageChunk applicationMessageChunk = ApplicationMessageChunk.newBuilder()
                    .setApplicationMessageId(transactionID)
                    .setChunkIndex(partId)
                    .setChunksCount(totalParts)
                    .setMessageChunk(ByteString.copyFrom(amMessageChunk))
                    .build();
            
            parts.add(applicationMessageChunk);
        }
        return parts;
    }
}
