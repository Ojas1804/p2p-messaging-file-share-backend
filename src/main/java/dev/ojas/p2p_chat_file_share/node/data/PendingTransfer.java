package dev.ojas.p2p_chat_file_share.node.data;

import lombok.Data;

import java.util.List;

@Data
public class PendingTransfer {
    String fileId;
    List<Integer> receivedChunks;
}