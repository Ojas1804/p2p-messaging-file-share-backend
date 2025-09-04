package dev.ojas.p2p_chat_file_share.node.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.ojas.p2p_chat_file_share.utils.CryptoUtils;
import lombok.Getter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class Vault {
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int PBKDF2_ITER = 200_000; // tune for your environment

    public static EncryptedSeed encryptSeed(byte[] seed, char[] password) {
        try {
            byte[] salt = CryptoUtils.randomBytes(SALT_LEN);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITER, 256);
            SecretKey derived = skf.generateSecret(spec);
            byte[] keyBytes = derived.getEncoded();
            SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = CryptoUtils.randomBytes(IV_LEN);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcm);
            byte[] ct = cipher.doFinal(seed);

            return new EncryptedSeed(
                    Base64.getEncoder().encodeToString(salt),
                    PBKDF2_ITER,
                    Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(ct)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] decryptSeed(EncryptedSeed es, char[] password) {
        try {
            byte[] salt = Base64.getDecoder().decode(es.salt);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password, salt, es.iterations, 256);
            SecretKey derived = skf.generateSecret(spec);
            byte[] keyBytes = derived.getEncoded();
            SecretKeySpec aesKey = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = Base64.getDecoder().decode(es.iv);
            byte[] ct = Base64.getDecoder().decode(es.ciphertext);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcm = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcm);
            return cipher.doFinal(ct);
        } catch (Exception e) {
            throw new RuntimeException("bad password or corrupted vault", e);
        }
    }

    @Getter
    public static class EncryptedSeed {
        public final String salt;
        public final int iterations;
        public final String iv;
        public final String ciphertext;

        @JsonCreator
        public EncryptedSeed(@JsonProperty("salt") String salt,
                             @JsonProperty("iterations") int iterations,
                             @JsonProperty("iv") String iv,
                             @JsonProperty("ciphertext") String ciphertext) {
            this.salt = salt;
            this.iterations = iterations;
            this.iv = iv;
            this.ciphertext = ciphertext;
        }
    }
}
