import java.security.DrbgParameters;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static java.security.DrbgParameters.Capability.RESEED_ONLY;

/**
 * Como gerar valores seguros com SecureRandom.
 * Rode com Java 17+ :  java src/GeracaoSegura.java
 */
public class GeracaoSegura {

    // UMA instancia, semeada uma vez, reutilizada (thread-safe).
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static void main(String[] args) throws NoSuchAlgorithmException {
        // 1) Algoritmo padrao depende do provider/SO (em Linux, normalmente NativePRNG).
        System.out.println("algoritmo padrao = " + SECURE_RANDOM.getAlgorithm());

        // 2) Token de 128 bits em hexadecimal (32 chars).
        System.out.println("codigo (hex)    = " + gerarCodigoHex());

        // 3) Mesmos 128 bits em Base64 URL-safe (22 chars), bom para URLs.
        System.out.println("codigo (base64) = " + gerarCodigoBase64Url());

        // 4) UUID v4 ja usa SecureRandom internamente (122 bits efetivos).
        System.out.println("uuid v4         = " + UUID.randomUUID());

        // 5) DRBG (NIST SP 800-90A) com forca de 128 bits, quando ha requisito de conformidade.
        SecureRandom drbg = SecureRandom.getInstance(
                "DRBG", DrbgParameters.instantiation(128, RESEED_ONLY, null));
        System.out.println("algoritmo drbg  = " + drbg.getAlgorithm());
    }

    /** 16 bytes aleatorios -> 32 hex chars (128 bits de entropia). */
    static String gerarCodigoHex() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    /** Mesma entropia, codificacao mais compacta e segura para URL. */
    static String gerarCodigoBase64Url() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
