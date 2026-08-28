package br.gov.crateus.bcm.saged.api;

import br.gov.crateus.bcm.saged.application.DemandService;
import br.gov.crateus.bcm.saged.config.SagedTelegramProperties;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterStatus;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramRequesterRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "saged-telegram-miniapp")
public class TelegramMiniAppController {

    private final TelegramRequesterRepository requesterRepository;
    private final DemandService demandService;
    private final SagedTelegramProperties props;

    public TelegramMiniAppController(TelegramRequesterRepository requesterRepository,
                                      DemandService demandService,
                                      SagedTelegramProperties props) {
        this.requesterRepository = requesterRepository;
        this.demandService = demandService;
        this.props = props;
    }

    @GetMapping(value = "/telegram/app", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> miniApp(HttpServletResponse response) throws IOException {
        response.setHeader("ngrok-skip-browser-warning", "69420");
        response.setHeader("Cache-Control", "no-store");
        byte[] html = StreamUtils.copyToByteArray(new ClassPathResource("saged-miniapp/app.html").getInputStream());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @GetMapping(value = "/telegram/info", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<byte[]> infoPage(HttpServletResponse response) throws IOException {
        response.setHeader("ngrok-skip-browser-warning", "69420");
        response.setHeader("Cache-Control", "no-store");
        byte[] html = StreamUtils.copyToByteArray(new ClassPathResource("saged-miniapp/info.html").getInputStream());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    // /api/v1/saged/webhooks/** is public via DevHostSecurityConfig
    @PostMapping("/api/v1/saged/webhooks/miniapp/demand")
    public ResponseEntity<Map<String, String>> createDemand(@RequestBody MiniAppDemandRequest request,
                                                             HttpServletResponse response) {
        response.setHeader("ngrok-skip-browser-warning", "69420");

        if (request.getInitData() == null || request.getInitData().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "initData is required");
        }

        String telegramUserId = validateInitDataAndExtractUserId(request.getInitData());

        var opt = requesterRepository.findByTelegramChatId(telegramUserId);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not registered in SAGED");
        }
        if (opt.get().getStatus() != TelegramRequesterStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User account is not active");
        }
        TelegramRequesterEntity requester = opt.get();

        DemandEntity demand = demandService.create(
            request.getTitle(),
            "Criado via Telegram",
            request.getSpecialtyCode(),
            null,
            requester.getId(),
            requester.getDepartmentId(),
            "telegram-app:" + telegramUserId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("protocol", demand.getProtocol()));
    }

    private String validateInitDataAndExtractUserId(String initData) {
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

    public static class MiniAppDemandRequest {
        private String initData;
        private String specialtyCode;
        private String title;

        public String getInitData()           { return initData; }
        public void setInitData(String v)     { this.initData = v; }
        public String getSpecialtyCode()      { return specialtyCode; }
        public void setSpecialtyCode(String v){ this.specialtyCode = v; }
        public String getTitle()              { return title; }
        public void setTitle(String v)        { this.title = v; }
    }
}
