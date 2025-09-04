package dev.ojas.p2p_chat_file_share.file;

import dev.ojas.p2p_chat_file_share.message.data.FileChunkMessage;
import dev.ojas.p2p_chat_file_share.utils.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;

/**
 * FileChunker: read file, produce encrypted chunk messages using AES-GCM with provided fileKey.
 * - chunk storage: <workDir>/<fileId>.chunk.<index>
 * - encrypted format for chunk payload: BASE64(iv || ciphertext)
 */
@Component
public class FileChunker {
    @Autowired
    private Environment environment;
    private final int chunkSize;
    private final byte[] fileKey; // 32 bytes AES key
    private final Path workDir;

    public FileChunker(int chunkSize, byte[] fileKey, Path workDir) throws NullPointerException {
        int maxChunkSize = Integer.parseInt(Objects.requireNonNull(environment.getProperty("p2pcf.file.max-chunk-size")));
        this.chunkSize = Math.min(chunkSize, maxChunkSize);
        this.fileKey = fileKey.clone();
        this.workDir = workDir;
        File d = workDir.toFile();
        if (!d.exists()) d.mkdirs();
    }

    public FileChunker(int chunkSize, byte[] fileKey, String workDirPathString) throws NullPointerException {
        int maxChunkSize = Integer.parseInt(Objects.requireNonNull(environment.getProperty("p2pcf.file.max-chunk-size")));
        this.chunkSize = Math.min(chunkSize, maxChunkSize);
        this.fileKey = fileKey.clone();
        this.workDir = Paths.get(workDirPathString);
        File d = this.workDir.toFile();
        if (!d.exists()) d.mkdirs();
    }

    public FileChunker(byte[] fileKey, Path workDir) throws NullPointerException{
        this.chunkSize = Integer.parseInt(Objects.requireNonNull(environment.getProperty("p2pcf.file.default-chunk-size")));
        this.fileKey = fileKey;
        this.workDir = workDir;
        File d = workDir.toFile();
        if(!d.exists()) d.mkdirs();
    }

    public FileChunker(byte[] fileKey, String workDirPathString) throws NullPointerException {
        this.chunkSize = Integer.parseInt(Objects.requireNonNull(environment.getProperty("p2pcf.file.default-chunk-size")));
        this.fileKey = fileKey;
        this.workDir = Paths.get(workDirPathString);
        File d = this.workDir.toFile();
        if(!d.exists()) d.mkdirs();
    }

    /**
     * Read local file and produce chunk files on disk and optionally a FileChunkMessage for each chunk.
     * Caller can iterate indices and call buildChunkMessage(index) to get the POJO to send.
     */
    public int chunkFile(Path filePath, String fileId) throws Exception {
        File f = filePath.toFile();
        if (!f.exists()) throw new IllegalArgumentException("file not found: " + filePath);
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] buf = new byte[chunkSize];
            int read;
            int idx = 0;
            while ((read = fis.read(buf)) != -1) {
                byte[] plain = (read == buf.length) ? buf : java.util.Arrays.copyOf(buf, read);
                // encrypt chunk
                java.util.Map<String, String> enc = CryptoUtils.aesGcmEncryptToBase64(fileKey, plain);
                // combine iv + ct into single Base64 payload for transport (iv bytes + ciphertext bytes)
                byte[] iv = CryptoUtils.fromBase64(enc.get("iv"));
                byte[] ct = CryptoUtils.fromBase64(enc.get("ct"));
                byte[] merged = new byte[iv.length + ct.length];
                System.arraycopy(iv, 0, merged, 0, iv.length);
                System.arraycopy(ct, 0, merged, iv.length, ct.length);
                String mergedB64 = Base64.getEncoder().encodeToString(merged);

                // persist chunk to disk for resume / resend
                Path chunkPath = workDir.resolve(fileId + ".chunk." + idx);
                java.nio.file.Files.write(chunkPath, merged);

                idx++;
            }
            return idx; // total chunks
        }
    }

    /**
     * Build a FileChunkMessage for a chunk index by reading the persisted chunk file (so we don't re-encrypt).
     */
    public FileChunkMessage buildChunkMessage(String fileId, int chunkIndex, int totalChunks, String fromNodeId, String toNodeId) throws Exception {
        Path chunkPath = workDir.resolve(fileId + ".chunk." + chunkIndex);
        if (!chunkPath.toFile().exists()) throw new IllegalArgumentException("missing chunk file: " + chunkPath);

        byte[] merged = java.nio.file.Files.readAllBytes(chunkPath);
        // merged is iv||ciphertext; encode as BASE64 string for FileChunkMessage.encryptedChunkData
        String mergedB64 = Base64.getEncoder().encodeToString(merged);

        FileChunkMessage fcm = new FileChunkMessage();
        fcm.setFrom(fromNodeId);
        fcm.setTo(toNodeId);
        fcm.setFileId(fileId);
        fcm.setChunkIndex(chunkIndex);
        fcm.setTotalChunks(totalChunks);
        // chunkHash optionally compute SHA-256 of merged bytes
        fcm.setChunkHash(CryptoUtils.toHex(CryptoUtils.sha256(merged)));
        fcm.setEncryptedChunkData(mergedB64);
        return fcm;
    }
}
