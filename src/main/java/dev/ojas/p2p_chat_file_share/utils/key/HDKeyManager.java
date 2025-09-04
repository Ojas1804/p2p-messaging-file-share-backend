package dev.ojas.p2p_chat_file_share.utils.key;

import dev.ojas.p2p_chat_file_share.utils.CryptoUtils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;

import java.math.BigInteger;
import java.security.*;
import java.util.Locale;

import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

public class HDKeyManager {
    static {
        // ensure BouncyCastle provider is available
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    private final byte[] masterSeed;

    /**
     * masterSeed MUST be kept secret (encrypted at rest). Provide bytes from Vault.decryptSeed(...)
     */
    public HDKeyManager(byte[] masterSeed) {
        this.masterSeed = masterSeed.clone();
    }

    /**
     * Deterministic, path-based derivation. This is NOT BIP32; it's a simpler HMAC-based deterministic scheme:
     * iterate HMAC(masterSeed, pathSegment) for each segment, chaining result.
     * Path format: "m/999'/0'/1/3"  (we'll treat segments as strings and HMAC them)
     */
    public byte[] deriveBytesForPath(String path) {
        if (path == null || path.isEmpty()) throw new IllegalArgumentException("path required");
        String[] parts = path.trim().toLowerCase(Locale.ROOT).split("/");
        byte[] key = masterSeed;
        for (String p : parts) {
            if (p.equals("m") || p.equals("")) continue;
            // canonicalize segment bytes
            byte[] seg = p.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            key = CryptoUtils.hmacSha256(key, seg);
        }
        return key;
    }

    /**
     * Deterministically derive an EC secp256k1 KeyPair from the path-derived bytes.
     * Uses SHA-256(derivedBytes) as the private scalar (reduced mod N).
     */
    public KeyPair deriveECKeyPair(String path) {
        try {
            byte[] derived = deriveBytesForPath(path);
            byte[] privCandidate = CryptoUtils.sha256(derived); // 32 bytes
            BigInteger d = new BigInteger(1, privCandidate);

            // fetch curve spec
            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("secp256k1");
            BigInteger n = spec.getN();

            // reduce scalar into [1, n-1]
            d = d.mod(n.subtract(BigInteger.ONE)).add(BigInteger.ONE);

            // create priv/public using BouncyCastle KeyFactory
            ECPrivateKeySpec privSpec = new ECPrivateKeySpec(d, spec);
            KeyFactory kf = KeyFactory.getInstance("EC", "BC");
            PrivateKey privKey = kf.generatePrivate(privSpec);

            // compute public point Q = G * d
            ECPoint q = spec.getG().multiply(d);
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(q, spec);
            PublicKey pubKey = kf.generatePublic(pubSpec);

            return new KeyPair(pubKey, privKey);
        } catch (Exception e) {
            throw new RuntimeException("deriveECKeyPair failed", e);
        }
    }

    /**
     * Derive a symmetric key (32 bytes) from the path using HKDF: HKDF-Extract(masterSeed) then Expand(info=path)
     */
    public byte[] deriveSymmetricKey(String path, byte[] info) {
        byte[] prk = CryptoUtils.hkdfExtract(null, masterSeed);
        byte[] infoBytes = (info == null) ? path.getBytes(java.nio.charset.StandardCharsets.UTF_8) : info;
        return CryptoUtils.hkdfExpand(prk, infoBytes, 32);
    }
}
