package dev.ojas.p2p_chat_file_share.message.data;

public class FileMetadataMessage extends BaseMessage {
    private String fileId;
    private String fileName;
    private long fileSize;
    private int chunkSize;
    private int totalChunks;
    private String fileHash;
    private String lockedFileKey;
    private String signature;

    public FileMetadataMessage() {
        super(MessageType.FILE_METADATA_MESSAGE);
    }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getLockedFileKey() { return lockedFileKey; }
    public void setLockedFileKey(String lockedFileKey) { this.lockedFileKey = lockedFileKey; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}
