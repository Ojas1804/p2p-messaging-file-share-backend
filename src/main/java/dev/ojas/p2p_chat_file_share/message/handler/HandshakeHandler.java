package dev.ojas.p2p_chat_file_share.message.handler;

import dev.ojas.p2p_chat_file_share.config.StorageProperties;
import dev.ojas.p2p_chat_file_share.message.data.HandshakeAckMessage;
import dev.ojas.p2p_chat_file_share.message.data.HandshakeMessage;
import dev.ojas.p2p_chat_file_share.node.data.Node;
import dev.ojas.p2p_chat_file_share.node.data.Peer;
import dev.ojas.p2p_chat_file_share.utils.CryptoUtils;
import dev.ojas.p2p_chat_file_share.utils.persist.PersistenceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class HandshakeHandler {
    private final Node node;
    private final Path walletPath;

    @Autowired
    private StorageProperties storageProperties;
    @Autowired
    private PersistenceManager persistenceManager;

    public HandshakeHandler(Node node) {
        this.node = node;
        String walletPathString = storageProperties.getDir();
        this.walletPath = Paths.get(walletPathString);
    }

    /**
     * Handles incoming handshake messages
     * @param handshakeMsg The incoming handshake message
     * @param senderIp The IP address of the sender
     * @param senderPort The port of the sender
     * @return HandshakeAckMessage if successful, null if failed
     */
    public HandshakeAckMessage handleIncomingHandshake(HandshakeMessage handshakeMsg,
                                                       String senderIp, int senderPort) {
        try {
            // 1. Basic validation
            if (handshakeMsg.getFrom() == null || handshakeMsg.getEphemeralPubKey() == null
                    || handshakeMsg.getSignature() == null) {
                System.err.println("Invalid handshake message: missing required fields");
                return null;
            }

            // 2. Check if peer already exists
            if (isPeerAlreadyKnown(handshakeMsg.getFrom())) {
                System.out.println("Peer " + handshakeMsg.getFrom() + " already exists, updating last seen");
                updatePeerLastSeen(handshakeMsg.getFrom());
                return createHandshakeAck(handshakeMsg.getFrom());
            }

            // 3. Verify the signature
            if (!verifyHandshakeSignature(handshakeMsg)) {
                System.err.println("Handshake signature verification failed for peer: " + handshakeMsg.getFrom());
                return null;
            }

            // 4. Create new peer entry
            Peer newPeer = createNewPeer(handshakeMsg.getFrom(), senderIp, senderPort);

            // 5. Add peer to the node's peer list
            addPeerToNode(newPeer);

            // 6. Save the updated node data
            if (!saveNodeData()) {
                System.err.println("Failed to save node data after adding new peer");
                // Rollback - remove the peer we just added
                removePeerFromNode(newPeer.getNodeId());
                return null;
            }

            System.out.println("Successfully added new peer: " + handshakeMsg.getFrom());

            // 7. Create and return acknowledgment
            return createHandshakeAck(handshakeMsg.getFrom());

        } catch (Exception e) {
            System.err.println("Error handling handshake message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a peer with the given nodeId already exists
     */
    private boolean isPeerAlreadyKnown(String nodeId) {
        if (node.getPeers() == null) {
            return false;
        }

        for (Peer peer : node.getPeers()) {
            if (peer.getNodeId().equals(nodeId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the lastSeen timestamp for an existing peer
     */
    private void updatePeerLastSeen(String nodeId) {
        if (node.getPeers() == null) {
            return;
        }

        for (Peer peer : node.getPeers()) {
            if (peer.getNodeId().equals(nodeId)) {
                peer.setLastSeen(LocalDateTime.now()); // Unix timestamp
                saveNodeData(); // Save the updated timestamp
                break;
            }
        }
    }

    /**
     * Verifies the handshake message signature
     */
    private boolean verifyHandshakeSignature(HandshakeMessage handshakeMsg) {
        try {
            // Decode the ephemeral public key
            PublicKey ephemeralPubKey = CryptoUtils.decodePublicKey(handshakeMsg.getEphemeralPubKey());

            // Create the message to verify (same format used when signing)
            String messageToVerify = createSignatureMessage(handshakeMsg);

            // Verify the signature
            return CryptoUtils.verifyFromBase64(ephemeralPubKey, messageToVerify, handshakeMsg.getSignature());

        } catch (Exception e) {
            System.err.println("Error verifying handshake signature: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates the message string that was signed (should match sender's format)
     */
    private String createSignatureMessage(HandshakeMessage handshakeMsg) {
        // This should match the format used by the sender when creating the signature
        // Typically includes: nodeId, ephemeral public key, timestamp, and optionally nonce
        return String.format("HANDSHAKE:%s:%s:%d",
                handshakeMsg.getFrom(),
                handshakeMsg.getEphemeralPubKey(),
                handshakeMsg.getTimestamp());
    }

    /**
     * Creates a new peer object
     */
    private Peer createNewPeer(String nodeId, String ip, int port) {
        return new Peer(nodeId, ip, port, LocalDateTime.now());
    }

    /**
     * Adds a peer to the node's peer list
     */
    private void addPeerToNode(Peer newPeer) {
        if (node.getPeers() == null) {
            node.setPeers(new Peer[]{newPeer});
        } else {
            // Convert array to list, add new peer, convert back to array
            List<Peer> peerList = new ArrayList<>(Arrays.asList(node.getPeers()));
            peerList.add(newPeer);
            node.setPeers(peerList.toArray(new Peer[0]));
        }
    }

    /**
     * Removes a peer from the node's peer list (used for rollback)
     */
    private void removePeerFromNode(String nodeId) {
        if (node.getPeers() == null) {
            return;
        }

        List<Peer> peerList = new ArrayList<>(Arrays.asList(node.getPeers()));
        peerList.removeIf(peer -> peer.getNodeId().equals(nodeId));
        node.setPeers(peerList.toArray(new Peer[0]));
    }

    /**
     * Saves the node data to persistent storage
     */
    private boolean saveNodeData() {
        try {
            persistenceManager.saveWallet(node, walletPath);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save node data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates a handshake acknowledgment message
     */
    private HandshakeAckMessage createHandshakeAck(String peerNodeId) {
        try {
            HandshakeAckMessage ackMsg = new HandshakeAckMessage();
            ackMsg.setFrom(node.getIdentity().getNodeId());
            ackMsg.setTo(peerNodeId);

            // Generate ephemeral key pair for this session
            // You might want to store this temporarily for the session
            java.security.KeyPair ephemeralKeyPair = generateEphemeralKeyPair();
            ackMsg.setEphemeralPubKey(CryptoUtils.encodePublicKey(ephemeralKeyPair.getPublic()));

            // Sign the acknowledgment
            String messageToSign = String.format("HANDSHAKE_ACK:%s:%s:%d",
                    ackMsg.getFrom(),
                    ackMsg.getEphemeralPubKey(),
                    ackMsg.getTimestamp());

            // Use master private key for signing (you'll need to derive this from masterSeedEnc)
            // This is a simplified version - you'll need to decrypt masterSeedEnc first
            ackMsg.setSignature(signAckMessage(messageToSign));

            return ackMsg;

        } catch (Exception e) {
            System.err.println("Error creating handshake acknowledgment: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates an ephemeral key pair for the session
     */
    private java.security.KeyPair generateEphemeralKeyPair() throws Exception {
        java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        return keyGen.generateKeyPair();
    }

    /**
     * Signs the acknowledgment message using the node's master key
     * Note: You'll need to implement proper key derivation from masterSeedEnc
     */
    private String signAckMessage(String message) throws Exception {
        // This is a placeholder - you'll need to:
        // 1. Decrypt the masterSeedEnc using user's password
        // 2. Derive the appropriate private key using HD wallet derivation
        // 3. Sign the message with that key

        // For now, returning a placeholder
        throw new UnsupportedOperationException("Implement key derivation and signing");
    }
}