package dev.ojas.p2p_chat_file_share.room;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

public class Room {
    @Setter
    @Getter
    private String roomId;               // unique room ID
    @Setter
    @Getter
    private String creator;              // userId of room creator
    @Setter
    @Getter
    private String encryptedRoomKey;     // encrypted with current user’s pubkey
    @Setter
    @Getter
    private byte[] roomKey;
    @Getter// decrypted symmetric key (AES) for encrypting chat/files
    private Set<String> members;         // userIds of peers in the room
    @Getter
    private long createdAt;

    public Room(String roomId, String creator, String encryptedRoomKey, byte[] roomKey) {
        this.roomId = roomId;
        this.creator = creator;
        this.encryptedRoomKey = encryptedRoomKey;
        this.roomKey = roomKey;
        this.members = new HashSet<>();
        this.members.add(creator);
        this.createdAt = System.currentTimeMillis();
    }
    public void addMember(String userId) { this.members.add(userId); }
    public void removeMember(String userId) { this.members.remove(userId); }
}
