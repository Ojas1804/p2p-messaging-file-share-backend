package dev.ojas.p2p_chat_file_share.message.data;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FileChunkMessage extends BaseMessage {
    private String fileId;
    private int chunkIndex;
    private int totalChunks;
    private String chunkHash;
    private String encryptedChunkData;
    private String signature;

    public FileChunkMessage() {
        super(MessageType.FILE_CHUNK_MESSAGE);
    }

    public int getChunkIndex() {
        return this.chunkIndex;
    }

    public String getEncryptedChunkData() {
        return this.encryptedChunkData;
    }

    public String getChunkHash() {
        return this.chunkHash;
    }

    public void setFrom(String from) {
        super.setFrom(from);
    }
}
