package br.gov.crateus.bcm.saged.infrastructure.telegram;

import br.gov.crateus.bcm.saged.config.SagedTelegramProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TelegramInitDataValidator {

    private final SagedTelegramProperties props;

    public TelegramInitDataValidator(SagedTelegramProperties props) {
        this.props = props;
    }

    public String extractUserId(String initData) {
        if (initData == null || initData.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "initData is required");
        }
        try {
            Map<String, String> params = new LinkedHashMap<>();
            for (String part : initData.split("&")) {
                int eq = part.indexOf('=');
                if (eq > 0) {
                    params.put(part.substring(0, eq),
                               URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8));
                }
            }

            String receivedHash = params.remove("hash");
            if (receivedHash == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "initData missing hash");
            }

            String dataCheckString = new TreeMap<>(params).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + "\n" + b)
                .orElseThrow();

            String botToken = props.getBotToken();
            if (botToken == null || botToken.isBlank()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Bot not configured");
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] secretKey = mac.doFinal(botToken.getBytes(StandardCharsets.UTF_8));

            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] computed = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
            byte[] received = HexFormat.of().parseHex(receivedHash);

            if (!MessageDigest.isEqual(computed, received)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "initData signature invalid");
            }

            String userJson = params.get("user");
            if (userJson == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user field missing in initData");
            }
            JsonNode node = new ObjectMapper().readTree(userJson);
            return node.get("id").asText();

        } catch (ResponseStatusException e) {
            throw e;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "HMAC error");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid initData: " + e.getMessage());
        }
    }
}
