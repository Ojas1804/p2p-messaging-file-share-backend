package dev.ojas.p2p_chat_file_share.message.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseMessage {
    private MessageType type;
    private String from;
    private String to;
    private long timestamp;
    private Long seq; // sequence number assigned by sender (nullable until set)

    public BaseMessage(MessageType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}
