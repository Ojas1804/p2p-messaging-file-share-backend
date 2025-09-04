package dev.ojas.p2p_chat_file_share.message.data;

import lombok.Getter;

@Getter
public class HandshakeAckMessage extends BaseMessage {
    // Getters and setters
    private String ephemeralPubKey;
    private String signature;

    public HandshakeAckMessage() {
        super(MessageType.HANDSHAKE_ACK);
    }

    public void setEphemeralPubKey(String ephemeralPubKey) { this.ephemeralPubKey = ephemeralPubKey; }

    public void setSignature(String signature) { this.signature = signature; }
}

