package dev.ojas.p2p_chat_file_share.message.data;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class HandshakeMessage extends BaseMessage {
    // base fields: type, from, to, timestamp
    private String ephemeralPubKey;  // Base64-encoded X.509 PUBLIC KEY (subjectPublicKeyInfo)
    private String signature;        // Base64(signature)
    private String nonce;
    // optional: you can include a nonce if you want replay protection
    public HandshakeMessage() {
        super(MessageType.HANDSHAKE_MESSAGE);
    }
}
