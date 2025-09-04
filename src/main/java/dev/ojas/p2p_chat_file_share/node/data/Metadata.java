package dev.ojas.p2p_chat_file_share.node.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Metadata {
    int knownPeersCount;
    int roomCount;
    LocalDateTime lastSeen;
}
