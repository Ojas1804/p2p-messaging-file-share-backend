package dev.ojas.p2p_chat_file_share.utils;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class CryptoUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtils() {}

    public static byte[] randomBytes(int len) {
        byte[] b = new byte[len];
        SECURE_RANDOM.nextBytes(b);
        return b;
    }

    public static byte[] sha256(byte[]... parts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (byte[] p : parts) md.update(p);
            return md.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKey sk = new SecretKeySpec(key, "HmacSHA256");
            mac.init(sk);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Basic HKDF-Expand (we'll use Extract as HMAC with salt, Expand via info)
    public static byte[] hkdfExtract(byte[] salt, byte[] ikm) {
        if (salt == null) salt = new byte[32]; // zeros
        return hmacSha256(salt, ikm);
    }

    public static byte[] hkdfExpand(byte[] prk, byte[] info, int outLen) {
        int hashLen = 32;
        int n = (outLen + hashLen - 1) / hashLen;
        byte[] result = new byte[outLen];
        byte[] t = new byte[0];
        int copied = 0;
        for (int i = 1; i <= n; i++) {
            byte[] input = new byte[t.length + (info == null ? 0 : info.length) + 1];
            System.arraycopy(t, 0, input, 0, t.length);
            if (info != null) System.arraycopy(info, 0, input, t.length, info.length);
            input[input.length - 1] = (byte) i;
            t = hmacSha256(prk, input);
            int want = Math.min(hashLen, outLen - copied);
            System.arraycopy(t, 0, result, copied, want);
            copied += want;
        }
        return result;
    }

    public static String toBase64(byte[] b) {
        return Base64.getEncoder().encodeToString(b);
    }

    public static byte[] fromBase64(String s) {
        return Base64.getDecoder().decode(s);
    }

    public static String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x & 0xff));
        return sb.toString();
    }

    public static Map<String, String> aesGcmEncryptToBase64(byte[] key, byte[] plaintext) {
        try {
            byte[] iv = randomBytes(12); // 12 bytes recommended for GCM
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcm);
            byte[] ct = cipher.doFinal(plaintext); // ct includes tag at the end
            Map<String, String> out = new HashMap<>();
            out.put("iv", toBase64(iv));
            out.put("ct", toBase64(ct));
            return out;
        } catch (Exception e) {
            throw new RuntimeException("aesGcmEncrypt failed", e);
        }
    }

    public static byte[] aesGcmDecryptFromBase64(byte[] key, String ivB64, String ctB64) {
        try {
            byte[] iv = fromBase64(ivB64);
            byte[] ct = fromBase64(ctB64);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcm);
            return cipher.doFinal(ct);
        } catch (Exception e) {
            throw new RuntimeException("aesGcmDecrypt failed", e);
        }
    }

    // === New: HKDF wrapper ===
    public static SecretKey hkdfSha256(byte[] ikm, byte[] info, int outLen) {
        byte[] prk = hkdfExtract(new byte[32], ikm); // salt = zeros
        byte[] okm = hkdfExpand(prk, info, outLen);
        return new SecretKeySpec(okm, "AES");
    }

    // === Key encoding/decoding ===
    public static String encodePublicKey(PublicKey pub) {
        return Base64.getEncoder().encodeToString(pub.getEncoded()); // X.509 SubjectPublicKeyInfo
    }

    public static PublicKey decodePublicKey(String b64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(b64);
        KeyFactory kf = KeyFactory.getInstance("EC"); // assuming secp256r1/secp256k1
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        return kf.generatePublic(spec);
    }

    public static String encodePrivateKey(PrivateKey priv) {
        return Base64.getEncoder().encodeToString(priv.getEncoded()); // PKCS#8
    }

    public static PrivateKey decodePrivateKey(String b64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(b64);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return kf.generatePrivate(spec);
    }

    // === Sign / Verify ===
    public static byte[] sign(PrivateKey priv, byte[] data) throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(priv);
        sig.update(data);
        return sig.sign();
    }

    public static boolean verifySignature(PublicKey pub, byte[] data, byte[] signature) throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initVerify(pub);
        sig.update(data);
        return sig.verify(signature);
    }

    // === Convenience for text signing ===
    public static String signToBase64(PrivateKey priv, String msg) throws Exception {
        byte[] sig = sign(priv, msg.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sig);
    }

    public static boolean verifyFromBase64(PublicKey pub, String msg, String sigB64) throws Exception {
        return verifySignature(pub,
                msg.getBytes(StandardCharsets.UTF_8),
                Base64.getDecoder().decode(sigB64));
    }
}