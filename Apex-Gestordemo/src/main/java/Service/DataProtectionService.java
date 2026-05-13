package Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Service
public class DataProtectionService {

    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+)");
    private static final Pattern DOCUMENT = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b|\\b\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\b");
    private static final Pattern PHONE = Pattern.compile("\\b(?:\\+?55\\s?)?(?:\\(?\\d{2}\\)?\\s?)?\\d{4,5}-?\\d{4}\\b");
    private static final Pattern BEARER = Pattern.compile("Bearer\\s+[A-Za-z0-9._~+/-]+=*", Pattern.CASE_INSENSITIVE);

    @Value("${apex.privacy.hash-pepper}")
    private String pepper;

    public String hash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((pepper + ":" + value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel", e);
        }
    }

    public String maskDocument(String value) {
        if (value == null || value.length() < 4) {
            return "***";
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "***" + digits;
        }
        return "***" + digits.substring(digits.length() - 4);
    }

    public String maskEmail(String value) {
        if (value == null || !value.contains("@")) {
            return "***";
        }
        String[] parts = value.split("@", 2);
        String first = parts[0].isBlank() ? "*" : parts[0].substring(0, 1);
        return first + "***@" + parts[1];
    }

    public String maskSensitiveText(String value) {
        if (value == null) {
            return null;
        }
        String masked = BEARER.matcher(value).replaceAll("Bearer ***");
        masked = EMAIL.matcher(masked).replaceAll("$1***$2");
        masked = DOCUMENT.matcher(masked).replaceAll("***DOCUMENTO***");
        masked = PHONE.matcher(masked).replaceAll("***TELEFONE***");
        return masked;
    }
}
