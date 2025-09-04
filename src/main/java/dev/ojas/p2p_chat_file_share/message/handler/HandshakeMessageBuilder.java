package dev.ojas.p2p_chat_file_share.message.handler;

import dev.ojas.p2p_chat_file_share.config.StorageProperties;
import dev.ojas.p2p_chat_file_share.message.data.HandshakeMessage;
import dev.ojas.p2p_chat_file_share.node.data.Node;
import dev.ojas.p2p_chat_file_share.node.data.Peer;
import dev.ojas.p2p_chat_file_share.node.data.Vault;
import dev.ojas.p2p_chat_file_share.utils.CryptoUtils;
import dev.ojas.p2p_chat_file_share.utils.persist.PersistenceManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicLong;

public class HandshakeMessageBuilder {
    private final Node node;
    private final Path walletPath;
    private final AtomicLong sequenceCounter;

    @Autowired
    private StorageProperties storageProperties;
    @Autowired
    private PersistenceManager persistenceManager;

    public HandshakeMessageBuilder(Node node) {
        this.node = node;
        String walletPathProperty = storageProperties.getDir();
        this.walletPath = Paths.get(walletPathProperty);
        this.sequenceCounter = new AtomicLong(0);
    }

    /**
     * Creates a complete handshake message for initiating connection with a peer
     * @param targetNodeId The nodeId of the peer we want to connect to
     * @return Complete HandshakeMessage ready to send
     */
    public HandshakeMessage createHandshakeMessage(String targetNodeId) throws Exception {
        // Generate ephemeral key pair for this handshake
        KeyPair ephemeralKeyPair = generateEphemeralKeyPair();

        // Create the base message
        HandshakeMessage handshake = new HandshakeMessage();
        handshake.setFrom(node.getIdentity().getNodeId());
        handshake.setTo(targetNodeId);
        handshake.setSeq(sequenceCounter.incrementAndGet());

        // Set ephemeral public key
        String ephemeralPubKeyB64 = CryptoUtils.encodePublicKey(ephemeralKeyPair.getPublic());
        handshake.setEphemeralPubKey(ephemeralPubKeyB64);

        // Create signature
        String messageToSign = createSignaturePayload(handshake);
        String signature = signHandshakeMessage(messageToSign, ephemeralKeyPair.getPrivate());
        handshake.setSignature(signature);

        // Update ephemeral index and save
        incrementEphemeralIndexAndSave();

        return handshake;
    }

    /**
     * Creates a handshake message with additional nonce for replay protection
     * @param targetNodeId The nodeId of the peer we want to connect to
     * @param nonce Optional nonce for replay protection
     * @return Complete HandshakeMessage with nonce
     */
    public HandshakeMessage createHandshakeMessageWithNonce(String targetNodeId, String nonce) throws Exception {
        HandshakeMessage handshake = createHandshakeMessage(targetNodeId);

        if (nonce != null && !nonce.isEmpty()) {
            handshake.setNonce(nonce);

            // Re-sign with nonce included
            String messageToSign = createSignaturePayload(handshake);
            KeyPair ephemeralKeyPair = generateEphemeralKeyPairFromIndex((int) (node.getIndices().getEphemeral() - 1));
            String signature = signHandshakeMessage(messageToSign, ephemeralKeyPair.getPrivate());
            handshake.setSignature(signature);
        }

        return handshake;
    }

    /**
     * Creates a handshake message for reconnection (using existing peer info)
     * @param peer The peer to reconnect to
     * @return Complete HandshakeMessage for reconnection
     */
    public HandshakeMessage createReconnectionHandshake(Peer peer) throws Exception {
        return createHandshakeMessage(peer.getNodeId());
    }

    /**
     * Generates a random nonce for replay protection
     * @return Base64 encoded random nonce
     */
    public String generateNonce() {
        byte[] nonceBytes = CryptoUtils.randomBytes(16); // 128-bit nonce
        return CryptoUtils.toBase64(nonceBytes);
    }

    /**
     * Creates the payload that will be signed
     * This must match the format expected by the receiver
     */
    private String createSignaturePayload(HandshakeMessage handshake) {
        StringBuilder payload = new StringBuilder();
        payload.append("HANDSHAKE:");
        payload.append(handshake.getFrom()).append(":");
        payload.append(handshake.getTo()).append(":");
        payload.append(handshake.getEphemeralPubKey()).append(":");
        payload.append(handshake.getTimestamp());

        // Include sequence number if present
        if (handshake.getSeq() != null) {
            payload.append(":").append(handshake.getSeq());
        }

        // Include nonce if present
        if (handshake.getNonce() != null && !handshake.getNonce().isEmpty()) {
            payload.append(":").append(handshake.getNonce());
        }

        return payload.toString();
    }

    /**
     * Signs the handshake message using the ephemeral private key
     */
    private String signHandshakeMessage(String message, PrivateKey ephemeralPrivateKey) throws Exception {
        return CryptoUtils.signToBase64(ephemeralPrivateKey, message);
    }

    /**
     * Generates an ephemeral key pair for this handshake
     */
    private KeyPair generateEphemeralKeyPair() throws Exception {
        int currentIndex = node.getIndices().getEphemeral();
        return generateEphemeralKeyPairFromIndex(currentIndex);
    }

    /**
     * Generates ephemeral key pair from specific index (for deterministic generation)
     */
    private KeyPair generateEphemeralKeyPairFromIndex(int index) throws Exception {
        // Get master seed
        byte[] masterSeed = decryptMasterSeed();

        // Create derivation info
        String derivationInfo = "ephemeral:" + index;
        byte[] derivationInfoBytes = derivationInfo.getBytes("UTF-8");

        // Derive ephemeral key material
        byte[] ephemeralSeed = CryptoUtils.hkdfExpand(
                CryptoUtils.hkdfExtract(null, masterSeed),
                derivationInfoBytes,
                32 // 256-bit seed
        );

        // Generate EC key pair from seed
        return createECKeyPairFromSeed(ephemeralSeed);
    }

    /**
     * Creates an EC key pair from seed bytes
     */
    private KeyPair createECKeyPairFromSeed(byte[] seed) throws Exception {
        // Create private key from seed
        PrivateKey privateKey = createECPrivateKeyFromBytes(seed);

        // Generate corresponding public key
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        java.security.spec.ECParameterSpec ecSpec = getP256ParameterSpec();
        keyGen.initialize(ecSpec);

        // Create public key from private key
        java.security.spec.ECPrivateKeySpec privSpec =
                new java.security.spec.ECPrivateKeySpec(
                        ((java.security.interfaces.ECPrivateKey) privateKey).getS(),
                        ecSpec);

        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("EC");

        // Calculate public key point
        java.security.spec.ECPoint publicPoint = calculatePublicPoint(
                ((java.security.interfaces.ECPrivateKey) privateKey).getS(),
                ecSpec);

        java.security.spec.ECPublicKeySpec pubSpec =
                new java.security.spec.ECPublicKeySpec(publicPoint, ecSpec);

        PublicKey publicKey = keyFactory.generatePublic(pubSpec);

        return new KeyPair(publicKey, privateKey);
    }

    /**
     * Calculates the public key point from private key scalar
     */
    private java.security.spec.ECPoint calculatePublicPoint(java.math.BigInteger privateKeyScalar,
                                                            java.security.spec.ECParameterSpec ecSpec) {
        // This is a simplified version - in production you might want to use a crypto library
        // like BouncyCastle for proper EC point multiplication
        java.security.spec.ECPoint generator = ecSpec.getGenerator();

        // For now, we'll use the KeyPairGenerator approach which is simpler
        // In a full implementation, you'd do: publicPoint = privateKeyScalar * generator

        // This is a placeholder - the actual EC point multiplication is complex
        // Consider using BouncyCastle or similar library for production
        throw new UnsupportedOperationException(
                "Implement EC point multiplication or use BouncyCastle library");
    }

    /**
     * Creates EC private key from bytes (same as in HandshakeHandler)
     */
    private PrivateKey createECPrivateKeyFromBytes(byte[] keyBytes) throws Exception {
        if (keyBytes.length != 32) {
            if (keyBytes.length < 32) {
                throw new IllegalArgumentException("Key material too short");
            }
            byte[] truncated = new byte[32];
            System.arraycopy(keyBytes, 0, truncated, 0, 32);
            keyBytes = truncated;
        }

        java.security.spec.ECParameterSpec ecSpec = getP256ParameterSpec();
        java.math.BigInteger privateKeyInt = new java.math.BigInteger(1, keyBytes);
        java.security.spec.ECPrivateKeySpec privateKeySpec =
                new java.security.spec.ECPrivateKeySpec(privateKeyInt, ecSpec);

        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(privateKeySpec);
    }

    /**
     * Gets P-256 parameter specification (same as in HandshakeHandler)
     */
    private java.security.spec.ECParameterSpec getP256ParameterSpec() throws Exception {
        java.security.AlgorithmParameters parameters = java.security.AlgorithmParameters.getInstance("EC");
        parameters.init(new java.security.spec.ECGenParameterSpec("secp256r1"));
        return parameters.getParameterSpec(java.security.spec.ECParameterSpec.class);
    }

    /**
     * Decrypts master seed (same implementation as HandshakeHandler)
     */
    private byte[] decryptMasterSeed() throws Exception {
        String password = getPasswordFromUser();
        Vault.EncryptedSeed seedEnc = node.getIdentity().getMasterSeedEnc();

        byte[] salt = CryptoUtils.fromBase64(seedEnc.getSalt());
        byte[] decryptionKey = deriveKeyFromPassword(password, salt, seedEnc.getIterations());

        // Handle IV extraction based on your encryption format
        String iv = extractIVFromCiphertext(seedEnc.getCiphertext());

        return CryptoUtils.aesGcmDecryptFromBase64(decryptionKey, iv, seedEnc.getCiphertext());
    }

    /**
     * Derives key from password using PBKDF2
     */
    private byte[] deriveKeyFromPassword(String password, byte[] salt, int iterations) throws Exception {
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(), salt, iterations, 256);
        javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.SecretKey key = factory.generateSecret(spec);
        return key.getEncoded();
    }

    /**
     * Increments the ephemeral index and saves the node data
     */
    private void incrementEphemeralIndexAndSave() throws Exception {
        node.getIndices().setEphemeral(node.getIndices().getEphemeral() + 1);
        persistenceManager.saveWallet(node, walletPath);
    }

    /**
     * Placeholder for password input
     */
    private String getPasswordFromUser() {
        throw new UnsupportedOperationException(
                "Implement password input mechanism for your application");
    }

    /**
     * Placeholder for IV extraction
     */
    private String extractIVFromCiphertext(String ciphertext) {
        throw new UnsupportedOperationException(
                "Implement IV extraction based on your encryption format");
    }
}