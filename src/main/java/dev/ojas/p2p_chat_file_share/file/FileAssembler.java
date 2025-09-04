package dev.ojas.p2p_chat_file_share.file;

import dev.ojas.p2p_chat_file_share.message.data.FileChunkMessage;
import dev.ojas.p2p_chat_file_share.utils.CryptoUtils;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * FileAssembler: store incoming encrypted chunk payloads on disk (as received), track missing chunks,
 * and reassemble when complete. Works with FileChunker persisted chunk files style (iv||ct).
 */
public class FileAssembler {
    private final Path workDir; // per-node working dir for chunks
    private final String fileId;
    private final int totalChunks;
    private final BitSet received;
    private final Path metaPath;

    public FileAssembler(Path workDir, String fileId, int totalChunks) throws Exception {
        this.workDir = workDir;
        this.fileId = fileId;
        this.totalChunks = totalChunks;
        this.received = new BitSet(totalChunks);
        this.metaPath = workDir.resolve(fileId + ".meta");
        loadMeta();
    }

    private void loadMeta() throws Exception {
        if (!metaPath.toFile().exists()) return;
        byte[] raw = java.nio.file.Files.readAllBytes(metaPath);
        String s = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
        if (s.trim().isEmpty()) return;
        String[] parts = s.split(",");
        for (String p : parts) {
            if (p.trim().isEmpty()) continue;
            received.set(Integer.parseInt(p.trim()));
        }
    }

    private void persistMeta() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = received.nextSetBit(0); i >= 0 && i < totalChunks; i = received.nextSetBit(i + 1)) {
            if (!sb.isEmpty()) sb.append(",");
            sb.append(i);
        }
        java.nio.file.Files.writeString(metaPath, sb.toString());
    }

    /**
     * Accept incoming FileChunkMessage (encryptedChunkData is Base64(iv||ct)).
     * Save chunk to disk and mark received.
     * Returns true if newly stored, false if duplicate/older.
     */
    public synchronized boolean acceptChunk(FileChunkMessage m) throws Exception {
        int idx = m.getChunkIndex();
        if (idx < 0 || idx >= totalChunks) throw new IllegalArgumentException("invalid chunk index");
        if (received.get(idx)) return false; // duplicate
        // save chunk file
        byte[] merged = Base64.getDecoder().decode(m.getEncryptedChunkData());
        Path chunkPath = workDir.resolve(fileId + ".chunk." + idx);
        java.nio.file.Files.write(chunkPath, merged);

        // optional verify chunkHash
        if (m.getChunkHash() != null && !m.getChunkHash().isEmpty()) {
            String gotHex = CryptoUtils.toHex(CryptoUtils.sha256(merged));
            if (!gotHex.equalsIgnoreCase(m.getChunkHash())) {
                // chunk corrupted: delete and return false
                java.nio.file.Files.deleteIfExists(chunkPath);
                throw new IllegalStateException("chunk hash mismatch for " + idx);
            }
        }

        received.set(idx);
        persistMeta();
        return true;
    }

    public synchronized Set<Integer> getMissingChunks() {
        Set<Integer> missing = new HashSet<>();
        for (int i = 0; i < totalChunks; i++) if (!received.get(i)) missing.add(i);
        return missing;
    }

    public synchronized Set<Integer> getReceivedSet() {
        Set<Integer> recvd = new HashSet<>();
        for (int i = received.nextSetBit(0); i >= 0 && i < totalChunks; i = received.nextSetBit(i + 1)) recvd.add(i);
        return recvd;
    }

    /**
     * Reassemble chunks into final output file path. Decrypts each chunk using fileKey before writing.
     * The caller must provide the same fileKey used by the sender to encrypt chunks.
     */
    public synchronized void assembleTo(Path outputFile, byte[] fileKey) throws Exception {
        if (received.cardinality() != totalChunks)
            throw new IllegalStateException("not all chunks received: have " + received.cardinality() + " of " + totalChunks);

        try (FileOutputStream out = new FileOutputStream(outputFile.toFile())) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkPath = workDir.resolve(fileId + ".chunk." + i);
                byte[] merged = java.nio.file.Files.readAllBytes(chunkPath);
                // split iv (12 bytes) and ciphertext
                byte[] iv = java.util.Arrays.copyOfRange(merged, 0, 12);
                byte[] ct = java.util.Arrays.copyOfRange(merged, 12, merged.length);
                // decrypt using CryptoUtils.aesGcmDecryptFromBase64, but it expects base64. So convert:
                String ivB64 = CryptoUtils.toBase64(iv);
                String ctB64 = CryptoUtils.toBase64(ct);
                byte[] plain = CryptoUtils.aesGcmDecryptFromBase64(fileKey, ivB64, ctB64);
                out.write(plain);
            }
        }
    }
}
