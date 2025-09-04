package dev.ojas.p2p_chat_file_share.message.data;

/**
 * Sent by receiver to inform sender which chunks have been received.
 * The 'received' field may be a bitmap-like compact comma list "0-4,6,8-10" or explicit indices.
 */
public class FileChunkAckMessage extends BaseMessage {
    private String fileId;
    private String received; // e.g. "0-3,5,7-9"
    private int totalChunks;

    public FileChunkAckMessage() { super(MessageType.FILE_CHUNK_ACK_MESSAGE); }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getReceived() { return received; }
    public void setReceived(String received) { this.received = received; }

    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
}
