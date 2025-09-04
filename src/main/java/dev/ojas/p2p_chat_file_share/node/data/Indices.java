package dev.ojas.p2p_chat_file_share.node.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
@AllArgsConstructor
public class Indices {
    private int chat;
    private int room;
    private int file;
    private int ephemeral;
}
