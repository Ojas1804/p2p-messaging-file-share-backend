package dev.ojas.p2p_chat_file_share.message.data;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RoomMessage extends BaseMessage {
    private String roomId;        // unique ID of the room
    private String event;         // "create", "join", "leave", "message"
    private String roomKey;       // encrypted symmetric key (optional, sent only on creation/invite)
    private String encryptedPayload; // for room chat messages
    private String signature;

    public RoomMessage() {
        super(MessageType.ROOM_MESSAGE);
    }
}
