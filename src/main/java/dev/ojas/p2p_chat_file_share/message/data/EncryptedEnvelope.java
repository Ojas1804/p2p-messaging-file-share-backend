package dev.ojas.p2p_chat_file_share.message.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Transport envelope used when sending encrypted messages.
 * {
 *   "type": "encrypted",
 *   "from": "nodeA",
 *   "to": "nodeB",
 *   "timestamp": 123456789,
 *   "iv": "BASE64_IV",
 *   "ciphertext": "BASE64_CIPHERTEXT"
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncryptedEnvelope {
    @Getter
    private String type = "encrypted";
    @Setter
    @Getter
    private String from;
    @Setter
    @Getter
    private String to;
    @Setter
    @Getter
    private long timestamp;
    @Setter
    @Getter
    private String iv;
    @Setter
    @Getter
    private String ciphertext;

    public EncryptedEnvelope() {
        this.timestamp = System.currentTimeMillis();
    }

}
