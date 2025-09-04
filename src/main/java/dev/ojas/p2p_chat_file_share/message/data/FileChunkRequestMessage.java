package dev.ojas.p2p_chat_file_share.message.data;

/**
 * Sent by receiver to request missing chunks explicitly.
 * 'missing' is a compact list "2,4-6,9".
 */
public class FileChunkRequestMessage extends BaseMessage {
    private String fileId;
    private String missing; // "2,4-6,9"
    private int totalChunks;

    public FileChunkRequestMessage() { super(MessageType.FILE_CHUNK_REQUEST_MESSAGE); }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getMissing() { return missing; }
    public void setMissing(String missing) { this.missing = missing; }

    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
}
