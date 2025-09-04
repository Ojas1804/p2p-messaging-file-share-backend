package dev.ojas.p2p_chat_file_share.message.factory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ojas.p2p_chat_file_share.message.data.*;

public class MessageFactory {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static BaseMessage fromJson(String json) throws Exception {
        JsonNode node = mapper.readTree(json);
        String type = node.get("type").asText();

        return switch (type) {
            case "handshake" -> mapper.treeToValue(node, HandshakeMessage.class);
            case "chat_message" -> mapper.treeToValue(node, ChatMessage.class);
            case "file_metadata" -> mapper.treeToValue(node, FileMetadataMessage.class);
            case "file_chunk" -> mapper.treeToValue(node, FileChunkMessage.class);
            case "room_message" -> mapper.treeToValue(node, RoomMessage.class);
            default -> throw new IllegalArgumentException("Unknown message type: " + type);
        };
    }
}
