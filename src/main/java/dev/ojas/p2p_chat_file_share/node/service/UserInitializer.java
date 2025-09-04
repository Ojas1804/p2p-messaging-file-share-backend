package dev.ojas.p2p_chat_file_share.node.service;

import dev.ojas.p2p_chat_file_share.config.StorageProperties;
import dev.ojas.p2p_chat_file_share.exception.StorageDirNullException;
import dev.ojas.p2p_chat_file_share.node.data.*;
import dev.ojas.p2p_chat_file_share.utils.CryptoUtils;
import dev.ojas.p2p_chat_file_share.utils.key.HDKeyManager;
import dev.ojas.p2p_chat_file_share.node.data.Vault;
import dev.ojas.p2p_chat_file_share.utils.persist.PersistenceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

@Service
public class UserInitializer {
    @Autowired
    private static Environment environment;
    @Autowired
    private static PersistenceManager persistenceManager;
    @Autowired
    private static StorageProperties storageProperties;
    private static int DISPLAY_NAME_LENGTH;

    public UserInitializer() {
        DISPLAY_NAME_LENGTH = Integer.parseInt(Objects.requireNonNull(environment.getProperty("p2pcf.user.name-length")));
    }

    public static void createNewUser(char[] password) throws Exception {
        String storageDir = storageProperties.getDir();
        if(storageDir == null) throw new StorageDirNullException("Storage directory not provided. Application side issue.");
        Path storageDirPath = Paths.get(storageDir);

        if(!Files.exists(storageDirPath)) Files.createDirectories(storageDirPath);
        // create new Identity
        String displayName = generateRandomString();

        // 1) master seed (32 bytes)
        byte[] masterSeed = CryptoUtils.randomBytes(32);

        // 2) create HD manager and derive a "master pub" to use as public identity (not xpub format)
        HDKeyManager km = new HDKeyManager(masterSeed);
        // derive an initial key as masterXPub surrogate
        KeyPair rootKP = km.deriveECKeyPair("m/999'/0'/0/0"); // canonical first child
        PublicKey pub = rootKP.getPublic();
        String masterXPubB64 = CryptoUtils.toBase64(pub.getEncoded());

        // 3) compute nodeId = SHA256(masterXPub)
        byte[] nodeIdHash = CryptoUtils.sha256(pub.getEncoded());
        String nodeIdHex = CryptoUtils.toHex(nodeIdHash);

        // 4) encrypt masterSeed with password
        Vault.EncryptedSeed encryptedSeed = Vault.encryptSeed(masterSeed, password);
        Identity identity = new Identity(displayName, nodeIdHex, encryptedSeed, masterXPubB64);

        // create Metadata
        Metadata metadata = new Metadata(1, 0, LocalDateTime.now());
        // indices
        Indices indices = new Indices(0, 0, 0, 0);
        // Peer
        Peer peer = new Peer("node1", "1.2.3.4", 9090, LocalDateTime.now());
        Peer[] peers = new Peer[metadata.getKnownPeersCount()];
        Arrays.fill(peers, peer);
        // config
        Config config = new Config(9090);
        Node node = new Node(identity, metadata, indices, peers, null, null, config);

        // Save new wallet
        persistenceManager.saveWallet(node, storageDirPath);
    }

    private static String generateRandomString() {
        // Define the character set (alphanumeric in this example)
        String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < DISPLAY_NAME_LENGTH; i++) {
            int index = random.nextInt(charSet.length());
            sb.append(charSet.charAt(index));
        }

        return sb.toString();
    }
}
