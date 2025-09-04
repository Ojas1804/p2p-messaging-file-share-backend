package dev.ojas.p2p_chat_file_share.room;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomManager {
    private Map<String, Room> rooms = new ConcurrentHashMap<>();

    public void createRoom(Room room) {
        rooms.put(room.getRoomId(), room);
    }

    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public void joinRoom(String roomId, String userId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.addMember(userId);
        }
    }

    public void leaveRoom(String roomId, String userId) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.removeMember(userId);
        }
    }

    public boolean isMember(String roomId, String userId) {
        Room room = rooms.get(roomId);
        return room != null && room.getMembers().contains(userId);
    }
}
