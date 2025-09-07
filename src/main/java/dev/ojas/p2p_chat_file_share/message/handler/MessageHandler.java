package dev.ojas.p2p_chat_file_share.message.handler;

import dev.ojas.p2p_chat_file_share.message.data.*;
import dev.ojas.p2p_chat_file_share.room.Room;
import dev.ojas.p2p_chat_file_share.room.RoomManager;
import org.springframework.beans.factory.annotation.Autowired;

public class MessageHandler {
    private final RoomManager roomManager;
    private final HandshakeHandler handshakeHandler;

    @Autowired
    public MessageHandler(RoomManager roomManager, HandshakeHandler handshakeHandler) {
        this.roomManager = roomManager;
        this.handshakeHandler = handshakeHandler;
    }

    public void handleMessage(BaseMessage msg, String ip, int port) {
        switch (msg.getType()) {
            case HANDSHAKE_MESSAGE:
                handshakeHandler.handleIncomingHandshake((HandshakeMessage) msg, ip, port);
                break;
            case HANDSHAKE_ACK_MESSAGE:
                break;
            case CHAT_MESSAGE:
                handleChat((ChatMessage) msg);
                break;
            case FILE_METADATA_MESSAGE:
                handleFileMetadata((FileMetadataMessage) msg);
                break;
            case FILE_CHUNK_ACK_MESSAGE:
                handleFileChunkAck((FileChunkAckMessage) msg);
                break;
            case FILE_CHUNK_REQUEST_MESSAGE:
                handleFileChunkRequest((FileChunkRequestMessage) msg);
                break;
            case FILE_CHUNK_MESSAGE:
                handleFileChunk((FileChunkMessage) msg);
                break;
            case ROOM_MESSAGE:
                handleRoom((RoomMessage) msg);
                break;
            default:
                System.out.println("Unknown message type: " + msg.getType());
        }
    }

    private void handleHandshake(HandshakeMessage msg) throws Exception {
        System.out.println("🤝 Handshake from " + msg.getFrom() + " with pubKey=" + msg.getEphemeralPubKey());
        // TODO: store peer’s identity


    }

    private void handleChat(ChatMessage msg) {
        System.out.println("💬 Chat from " + msg.getFrom() + ": " + msg.getEncryptedPayload());
        // TODO: decrypt payload with own private key
    }

    private void handleFileMetadata(FileMetadataMessage msg) {
        System.out.println("📂 File incoming from " + msg.getFrom() + ": " + msg.getFileName());
        // TODO: prepare to receive chunks
    }

    private void handleFileChunk(FileChunkMessage msg) {
        System.out.println("📦 Received chunk " + msg.getChunkIndex() + "/" + msg.getTotalChunks());
        // TODO: assemble chunks into file
    }

    private void handleRoom(RoomMessage msg) {
        String roomId = msg.getRoomId();
        switch (msg.getEvent()) {
            case "create":
                System.out.println("🏠 Room created by " + msg.getFrom() + ": " + roomId);
                // Decrypt room key with own private key
                byte[] roomKey = decryptRoomKey(msg.getRoomKey());
                Room room = new Room(roomId, msg.getFrom(), msg.getRoomKey(), roomKey);
                roomManager.createRoom(room);
                break;

            case "join":
                System.out.println("👤 " + msg.getFrom() + " joined room " + roomId);
                roomManager.joinRoom(roomId, msg.getFrom());
                break;

            case "leave":
                System.out.println("👋 " + msg.getFrom() + " left room " + roomId);
                roomManager.leaveRoom(roomId, msg.getFrom());
                break;

            case "message":
                System.out.println("💬 Room[" + roomId + "] " + msg.getFrom() + ": " + msg.getEncryptedPayload());
                // TODO: decrypt with room key
                break;

            default:
                System.out.println("❌ Unknown room event: " + msg.getEvent());
        }
    }

    private void handleFileChunkAck(FileChunkAckMessage msg) {

    }

    private void handleFileChunkRequest(FileChunkRequestMessage msg) {

    }

    private byte[] decryptRoomKey(String encryptedKey) {
        // TODO: implement RSA/ECC private key decryption
        return new byte[0];
    }
}
