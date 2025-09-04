package dev.ojas.p2p_chat_file_share.node.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@Getter
@AllArgsConstructor
public class Peer {
    String nodeId;
    String ip;
    int port;
    LocalDateTime lastSeen;
}