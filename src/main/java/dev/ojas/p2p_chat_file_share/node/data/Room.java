package dev.ojas.p2p_chat_file_share.node.data;

import lombok.Data;

@Data
public class Room {
    String roomId;
    String encryptedRoomKey;
}