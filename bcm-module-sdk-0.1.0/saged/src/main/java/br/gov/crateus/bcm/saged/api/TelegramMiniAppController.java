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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public String miniApp(HttpServletResponse response) {
        response.setHeader("ngrok-skip-browser-warning", "69420");
        response.setHeader("Cache-Control", "no-store");
        return buildHtml();
    }

    @GetMapping(value = "/telegram/info", produces = MediaType.TEXT_HTML_VALUE)
    public String infoPage(HttpServletResponse response) {
        response.setHeader("ngrok-skip-browser-warning", "69420");
        response.setHeader("Cache-Control", "no-store");
        return buildInfoHtml();
    }

    // /api/v1/saged/webhooks/** é público pelo DevHostSecurityConfig
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

    /**
     * Validates Telegram Web App initData using HMAC-SHA256 as per
     * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     * Returns the Telegram user ID if valid, throws 401 otherwise.
     */
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

    private static String buildHtml() {
        return """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
  <title>SAGED — Suporte de TI</title>
  <script src="https://telegram.org/js/telegram-web-app.js"></script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@tabler/icons-webfont@3.31.0/dist/tabler-icons.min.css"/>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    html, body {
      font-family: 'Inter', -apple-system, sans-serif;
      background: #f1f3f5;
      color: #212529;
      min-height: 100vh;
      -webkit-font-smoothing: antialiased;
    }
    .header {
      background: #1a4731;
      padding: 14px 20px 12px;
      display: flex; align-items: center; gap: 12px;
      position: sticky; top: 0; z-index: 10;
    }
    .header-logo {
      width: 38px; height: 38px;
      background: rgba(255,255,255,0.15); border-radius: 10px;
      display: flex; align-items: center; justify-content: center; font-size: 22px;
    }
    .header-text h1 { color: #fff; font-size: 15px; font-weight: 700; }
    .header-text p  { color: rgba(255,255,255,0.6); font-size: 11px; margin-top: 2px; }

    .page { display: none; padding: 20px 16px 32px; }
    .page.active { display: block; }

    .section-tag   { font-size: 10px; font-weight: 700; color: #2d9c5f; text-transform: uppercase; letter-spacing: 1.2px; margin-bottom: 6px; }
    .section-title { font-size: 20px; font-weight: 800; color: #1a1a2e; margin-bottom: 6px; }
    .section-sub   { font-size: 13px; color: #6c757d; margin-bottom: 24px; line-height: 1.6; }

    /* Apresentação */
    .intro-hero {
      background: linear-gradient(135deg, #1a4731, #2d9c5f);
      border-radius: 18px; padding: 28px 20px; text-align: center; margin-bottom: 22px;
    }
    .intro-hero .hero-icon { font-size: 52px; margin-bottom: 10px; }
    .intro-hero h2 { color: #fff; font-size: 22px; font-weight: 800; margin-bottom: 6px; }
    .intro-hero p  { color: rgba(255,255,255,0.8); font-size: 13px; line-height: 1.5; }

    .flow-steps { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
    .flow-step {
      background: #fff; border-radius: 12px; padding: 14px 16px;
      display: flex; align-items: center; gap: 14px;
      border-left: 4px solid #2d9c5f;
    }
    .flow-step .step-num {
      width: 32px; height: 32px; border-radius: 50%;
      background: #e8f7ef; color: #1a4731;
      font-size: 14px; font-weight: 800;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
    }
    .flow-step .step-text strong { display: block; font-size: 13px; font-weight: 700; color: #212529; }
    .flow-step .step-text span   { font-size: 12px; color: #868e96; }

    .info-note {
      background: #fff8e1; border: 1px solid #ffe082; border-radius: 10px;
      padding: 12px 14px; font-size: 12px; color: #795548; margin-bottom: 24px;
      display: flex; gap: 8px; align-items: flex-start;
    }
    .info-note i { font-size: 16px; color: #f59f00; flex-shrink: 0; margin-top: 1px; }

    /* Cards especialidade */
    .cards { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .spec-card {
      background: #fff; border: 2px solid #e9ecef; border-bottom: 4px solid #e9ecef;
      border-radius: 14px; padding: 22px 12px 18px; cursor: pointer; text-align: center;
      transition: border-color .15s, transform .15s, box-shadow .15s;
      display: flex; flex-direction: column; align-items: center; gap: 10px;
      -webkit-tap-highlight-color: transparent;
    }
    .spec-card:active { transform: scale(0.96); }
    .spec-card .icon-wrap {
      width: 56px; height: 56px; border-radius: 14px;
      background: #e8f7ef; display: flex; align-items: center; justify-content: center;
    }
    .spec-card .icon-wrap i { font-size: 28px; color: #2d9c5f; }
    .spec-card .label { font-size: 14px; font-weight: 700; color: #212529; }
    .spec-card .desc  { font-size: 11px; color: #868e96; line-height: 1.4; }
    .spec-card:hover, .spec-card.selected {
      border-color: #2d9c5f; border-bottom-color: #2d9c5f;
      box-shadow: 0 4px 16px rgba(45,156,95,.15);
    }

    /* Formulário */
    .selected-badge {
      display: flex; align-items: center; gap: 10px;
      background: #f0faf4; border: 1.5px solid #2d9c5f;
      border-radius: 12px; padding: 10px 14px; margin-bottom: 20px; cursor: pointer;
    }
    .selected-badge .badge-name { font-size: 14px; font-weight: 600; color: #1a4731; flex: 1; }
    .selected-badge .change { font-size: 11px; color: #2d9c5f; font-weight: 600; }

    label { display: block; font-size: 12px; font-weight: 600; color: #495057; margin-bottom: 6px; }
    .field { margin-bottom: 16px; }
    input[type=text], textarea {
      width: 100%; padding: 12px 14px;
      border: 1.5px solid #dee2e6; border-radius: 10px;
      font-size: 15px; font-family: inherit; color: #212529;
      background: #fff; outline: none; transition: border-color .2s;
      resize: none;
    }
    input[type=text]:focus, textarea:focus { border-color: #2d9c5f; }
    .optional-tag { font-size: 10px; color: #adb5bd; font-weight: 500; margin-left: 4px; }

    .btn {
      width: 100%; padding: 14px; border: none; border-radius: 12px;
      font-size: 15px; font-weight: 600; font-family: inherit;
      cursor: pointer; transition: opacity .15s;
    }
    .btn:active { opacity: .85; }
    .btn:disabled { opacity: .5; cursor: not-allowed; }
    .btn-primary { background: #1a4731; color: #fff; }
    .btn-outline  { background: #fff; color: #1a4731; border: 1.5px solid #1a4731; margin-bottom: 10px; }

    .error-box {
      background: #fff5f5; border: 1.5px solid #ffc9c9;
      border-radius: 10px; padding: 10px 14px;
      font-size: 13px; color: #c92a2a; margin-bottom: 14px; display: none;
    }
    .error-box.show { display: block; }

    /* Sucesso */
    .success-wrap {
      min-height: 65vh; display: flex; flex-direction: column;
      align-items: center; justify-content: center; text-align: center; padding: 24px;
    }
    .success-icon { font-size: 64px; margin-bottom: 16px; }
    .success-wrap h2 { font-size: 20px; font-weight: 800; margin-bottom: 8px; }
    .success-wrap p  { font-size: 14px; color: #6c757d; margin-bottom: 6px; }
    .protocol {
      background: #f0faf4; border: 1.5px solid #2d9c5f;
      border-radius: 10px; padding: 10px 20px;
      font-size: 18px; font-weight: 700; font-family: monospace;
      color: #1a4731; margin: 12px 0 24px; letter-spacing: 1px;
    }
  </style>
</head>
<body>
<div class="header">
  <div class="header-logo">🎫</div>
  <div class="header-text">
    <h1>SAGED</h1>
    <p>Suporte de TI — Prefeitura de Crateús</p>
  </div>
</div>

<!-- Página 0: Apresentação -->
<div class="page active" id="p0">
  <div class="intro-hero">
    <div class="hero-icon">🖥️</div>
    <h2>Suporte de TI</h2>
    <p>Abra chamados de manutenção e internet diretamente pelo Telegram, sem filas e sem ligações.</p>
  </div>

  <div class="section-tag">Como funciona</div>
  <div class="flow-steps">
    <div class="flow-step">
      <div class="step-num">1</div>
      <div class="step-text">
        <strong>Abra o chamado</strong>
        <span>Informe o tipo de problema e descreva brevemente.</span>
      </div>
    </div>
    <div class="flow-step">
      <div class="step-num">2</div>
      <div class="step-text">
        <strong>Técnico assume</strong>
        <span>Um técnico da Seplati recebe e assume o atendimento.</span>
      </div>
    </div>
    <div class="flow-step">
      <div class="step-num">3</div>
      <div class="step-text">
        <strong>Problema resolvido</strong>
        <span>Você recebe a confirmação assim que o chamado é concluído.</span>
      </div>
    </div>
  </div>

  <div class="info-note">
    <i class="ti ti-info-circle"></i>
    <span>O tombamento (número de patrimônio) do equipamento <strong>não é solicitado aqui</strong> — o técnico registra esse dado ao recolher o equipamento.</span>
  </div>

  <button class="btn btn-primary" onclick="show('p1')">Abrir Chamado</button>
</div>

<!-- Página 1: Especialidade -->
<div class="page" id="p1">
  <div class="section-tag">Passo 1 de 2</div>
  <div class="section-title">Tipo de atendimento</div>
  <div class="section-sub">Selecione a área do seu chamado.</div>
  <div class="cards">
    <div class="spec-card" onclick="pick('MANUT','ti-tool','Manutenção','Hardware e periféricos')">
      <div class="icon-wrap"><i class="ti ti-tool"></i></div>
      <div class="label">Manutenção</div>
      <div class="desc">Hardware e periféricos</div>
    </div>
    <div class="spec-card" onclick="pick('INTERNET','ti-wifi','Internet','Redes e conectividade')">
      <div class="icon-wrap"><i class="ti ti-wifi"></i></div>
      <div class="label">Internet</div>
      <div class="desc">Redes e conectividade</div>
    </div>
  </div>
</div>

<!-- Página 2: Formulário -->
<div class="page" id="p2">
  <div class="section-tag">Passo 2 de 2</div>
  <div class="section-title">Descreva o problema</div>
  <div class="section-sub">Quanto mais detalhes, mais rápido o atendimento.</div>

  <div class="selected-badge" onclick="show('p1')">
    <div class="icon-wrap" style="width:32px;height:32px;border-radius:8px;flex-shrink:0;background:#e8f7ef;display:flex;align-items:center;justify-content:center;">
      <i id="bIcon" class="ti" style="font-size:18px;color:#2d9c5f;"></i>
    </div>
    <span class="badge-name" id="bLabel"></span>
    <span class="change">Alterar</span>
  </div>

  <div class="error-box" id="errBox"></div>

  <div class="field">
    <label for="titleIn">Título do chamado *</label>
    <input type="text" id="titleIn" placeholder="Ex.: Computador da recepção não liga" maxlength="255"/>
  </div>

  <div class="field">
    <label for="descIn">Descrição <span class="optional-tag">opcional</span></label>
    <textarea id="descIn" rows="3" placeholder="Descreva melhor o problema, quando começou, mensagens de erro..."></textarea>
  </div>

  <button class="btn btn-primary" id="submitBtn" onclick="submit()">Abrir Chamado</button>
</div>

<!-- Página 3: Sucesso -->
<div class="page" id="p3">
  <div class="success-wrap">
    <div class="success-icon">✅</div>
    <h2>Chamado aberto!</h2>
    <p>Registrado com sucesso. Protocolo:</p>
    <div class="protocol" id="proto"></div>
    <p style="font-size:13px;color:#6c757d;margin-bottom:20px;">Um técnico entrará em contato em breve.</p>
    <button class="btn btn-primary" onclick="window.Telegram?.WebApp?.close()">Fechar</button>
  </div>
</div>

<script>
  const tg = window.Telegram?.WebApp;
  if (tg) { tg.ready(); tg.expand(); }

  const params = new URLSearchParams(window.location.search);
  const CHAT_ID = params.get('chatId') || '';

  let code = '', iconCls = '', lbl = '';

  function pick(c, i, l) {
    code = c; iconCls = i; lbl = l;
    document.getElementById('bIcon').className = 'ti ' + i;
    document.getElementById('bLabel').textContent = l;
    document.getElementById('titleIn').value = '';
    document.getElementById('descIn').value = '';
    document.getElementById('errBox').classList.remove('show');
    show('p2');
    setTimeout(() => document.getElementById('titleIn').focus(), 100);
  }

  function show(id) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById(id).classList.add('active');
    window.scrollTo(0, 0);
  }

  function submit() {
    const title = document.getElementById('titleIn').value.trim();
    const desc  = document.getElementById('descIn').value.trim();
    const err   = document.getElementById('errBox');
    const btn   = document.getElementById('submitBtn');
    if (!title) { err.textContent = 'Informe o título do chamado.'; err.classList.add('show'); return; }

    err.classList.remove('show');
    btn.disabled = true;
    btn.textContent = 'Enviando…';

    const payload = { specialtyCode: code, title: title };
    if (desc) payload.description = desc;

    try {
      tg.sendData(JSON.stringify(payload));
    } catch (e) {
      err.textContent = 'Erro ao enviar: ' + e.message;
      err.classList.add('show');
      btn.disabled = false;
      btn.textContent = 'Abrir Chamado';
    }
  }
</script>
</body>
</html>
""";
    }

    private static String buildInfoHtml() {
        return """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=1.0,user-scalable=no"/>
  <title>SAGED — Conheça o Sistema</title>
  <script src="https://telegram.org/js/telegram-web-app.js"></script>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@tabler/icons-webfont@3.31.0/dist/tabler-icons.min.css"/>
  <style>
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    html, body {
      font-family: 'Inter', -apple-system, sans-serif;
      background: #f1f3f5;
      color: #212529;
      min-height: 100vh;
      -webkit-font-smoothing: antialiased;
    }

    /* HERO */
    .hero {
      background: linear-gradient(160deg, #1a4731 0%, #2d9c5f 100%);
      padding: 36px 20px 32px;
      text-align: center;
      position: relative;
      overflow: hidden;
    }
    .hero::before {
      content: '';
      position: absolute; inset: 0;
      background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.04'%3E%3Ccircle cx='30' cy='30' r='30'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E") repeat;
      pointer-events: none;
    }
    .hero-badge {
      display: inline-block;
      background: rgba(255,255,255,0.15);
      color: rgba(255,255,255,0.9);
      font-size: 11px; font-weight: 700;
      text-transform: uppercase; letter-spacing: 1.5px;
      padding: 4px 12px; border-radius: 20px;
      margin-bottom: 14px;
    }
    .hero-icon-wrap {
      width: 72px; height: 72px; border-radius: 20px;
      background: rgba(255,255,255,0.15);
      display: flex; align-items: center; justify-content: center;
      margin: 0 auto 16px; font-size: 36px;
    }
    .hero h1 { color: #fff; font-size: 26px; font-weight: 800; line-height: 1.2; margin-bottom: 10px; }
    .hero p  { color: rgba(255,255,255,0.8); font-size: 14px; line-height: 1.6; max-width: 320px; margin: 0 auto; }

    /* STATS BAR */
    .stats-bar {
      display: grid; grid-template-columns: repeat(3, 1fr);
      background: #fff; border-bottom: 1px solid #f1f3f5;
    }
    .stat-item {
      padding: 14px 8px; text-align: center;
      border-right: 1px solid #f1f3f5;
    }
    .stat-item:last-child { border-right: none; }
    .stat-item .val { font-size: 20px; font-weight: 800; color: #1a4731; }
    .stat-item .lbl { font-size: 10px; font-weight: 600; color: #868e96; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 2px; }

    /* CONTENT */
    .content { padding: 20px 16px 100px; }

    .section { margin-bottom: 28px; }
    .section-label {
      font-size: 10px; font-weight: 700; color: #2d9c5f;
      text-transform: uppercase; letter-spacing: 1.2px; margin-bottom: 12px;
    }
    .section-title { font-size: 18px; font-weight: 800; color: #1a1a2e; margin-bottom: 8px; }
    .section-text  { font-size: 14px; color: #495057; line-height: 1.7; }

    /* FLOW STEPS */
    .steps { display: flex; flex-direction: column; gap: 0; }
    .step {
      display: flex; gap: 14px;
      padding: 16px 0;
      border-bottom: 1px solid #f1f3f5;
      position: relative;
    }
    .step:last-child { border-bottom: none; }
    .step-num-wrap { display: flex; flex-direction: column; align-items: center; flex-shrink: 0; }
    .step-num {
      width: 36px; height: 36px; border-radius: 50%;
      background: #1a4731; color: #fff;
      font-size: 14px; font-weight: 800;
      display: flex; align-items: center; justify-content: center;
      flex-shrink: 0;
    }
    .step-line {
      width: 2px; flex: 1; background: #e9ecef; margin-top: 4px;
      min-height: 20px;
    }
    .step:last-child .step-line { display: none; }
    .step-body { padding-top: 6px; }
    .step-body strong { display: block; font-size: 14px; font-weight: 700; color: #212529; margin-bottom: 4px; }
    .step-body span   { font-size: 13px; color: #6c757d; line-height: 1.5; }

    /* SERVICES */
    .services { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .service-card {
      background: #fff; border-radius: 14px; padding: 18px 14px;
      border: 1.5px solid #e9ecef;
      display: flex; flex-direction: column; align-items: flex-start; gap: 10px;
    }
    .service-icon {
      width: 44px; height: 44px; border-radius: 12px;
      background: #e8f7ef; display: flex; align-items: center; justify-content: center;
    }
    .service-icon i { font-size: 22px; color: #2d9c5f; }
    .service-name { font-size: 13px; font-weight: 700; color: #212529; }
    .service-desc { font-size: 12px; color: #868e96; line-height: 1.4; }

    /* HIGHLIGHT BOX */
    .highlight {
      background: #f0faf4; border: 1.5px solid #b2dfdb;
      border-radius: 14px; padding: 16px;
      display: flex; gap: 12px; align-items: flex-start;
    }
    .highlight i { font-size: 22px; color: #2d9c5f; flex-shrink: 0; margin-top: 2px; }
    .highlight-text strong { display: block; font-size: 13px; font-weight: 700; color: #1a4731; margin-bottom: 4px; }
    .highlight-text span   { font-size: 13px; color: #495057; line-height: 1.5; }

    /* BOTTOM CTA */
    .bottom-cta {
      position: fixed; bottom: 0; left: 0; right: 0;
      padding: 12px 16px 20px;
      background: #fff; border-top: 1px solid #e9ecef;
    }
    .btn {
      width: 100%; padding: 14px; border: none; border-radius: 12px;
      font-size: 15px; font-weight: 700; font-family: inherit;
      cursor: pointer; transition: opacity .15s;
      background: #1a4731; color: #fff;
    }
    .btn:active { opacity: .85; }
  </style>
</head>
<body>
  <script>
    const tg = window.Telegram?.WebApp;
    if (tg) { tg.ready(); tg.expand(); }
  </script>

  <!-- HERO -->
  <div class="hero">
    <div class="hero-badge">Prefeitura de Crateús · Seplati</div>
    <div class="hero-icon-wrap">🖥️</div>
    <h1>SAGED</h1>
    <p>Sistema de Gerenciamento de Demandas de Suporte de TI do município de Crateús.</p>
  </div>

  <!-- STATS -->
  <div class="stats-bar">
    <div class="stat-item"><div class="val">2</div><div class="lbl">Especialidades</div></div>
    <div class="stat-item"><div class="val">22</div><div class="lbl">Secretarias</div></div>
    <div class="stat-item"><div class="val">Sub-1h</div><div class="lbl">1º Atendimento</div></div>
  </div>

  <div class="content">

    <!-- O QUE É -->
    <div class="section">
      <div class="section-label">O que é</div>
      <div class="section-title">Suporte de TI direto no Telegram</div>
      <div class="section-text">
        O SAGED centraliza todos os chamados de suporte técnico da Prefeitura de Crateús.
        Servidores municipais podem abrir, acompanhar e encerrar chamados sem sair do Telegram —
        sem ligações, sem filas, sem papel.
      </div>
    </div>

    <!-- COMO FUNCIONA -->
    <div class="section">
      <div class="section-label">Como funciona</div>
      <div class="steps">
        <div class="step">
          <div class="step-num-wrap">
            <div class="step-num">1</div>
            <div class="step-line"></div>
          </div>
          <div class="step-body">
            <strong>Validar número</strong>
            <span>Compartilhe seu contato para vincular o seu número ao sistema. Um administrador libera seu acesso.</span>
          </div>
        </div>
        <div class="step">
          <div class="step-num-wrap">
            <div class="step-num">2</div>
            <div class="step-line"></div>
          </div>
          <div class="step-body">
            <strong>Abrir chamado</strong>
            <span>Escolha o tipo de suporte, informe o título e descreva o problema. Um protocolo único é gerado na hora.</span>
          </div>
        </div>
        <div class="step">
          <div class="step-num-wrap">
            <div class="step-num">3</div>
            <div class="step-line"></div>
          </div>
          <div class="step-body">
            <strong>Técnico assume</strong>
            <span>Um técnico da Seplati recebe o chamado, registra o equipamento e inicia o atendimento.</span>
          </div>
        </div>
        <div class="step">
          <div class="step-num-wrap">
            <div class="step-num">4</div>
            <div class="step-line"></div>
          </div>
          <div class="step-body">
            <strong>Problema resolvido</strong>
            <span>Você recebe a confirmação de conclusão com o protocolo do chamado.</span>
          </div>
        </div>
      </div>
    </div>

    <!-- SERVIÇOS -->
    <div class="section">
      <div class="section-label">Especialidades</div>
      <div class="services">
        <div class="service-card">
          <div class="service-icon"><i class="ti ti-tool"></i></div>
          <div>
            <div class="service-name">Manutenção</div>
            <div class="service-desc">Computadores, impressoras, periféricos e equipamentos de TI.</div>
          </div>
        </div>
        <div class="service-card">
          <div class="service-icon"><i class="ti ti-wifi"></i></div>
          <div>
            <div class="service-name">Internet</div>
            <div class="service-desc">Conectividade, roteadores, switches e infraestrutura de rede.</div>
          </div>
        </div>
      </div>
    </div>

    <!-- DESTAQUE -->
    <div class="section">
      <div class="highlight">
        <i class="ti ti-shield-check"></i>
        <div class="highlight-text">
          <strong>Rastreável e transparente</strong>
          <span>Cada chamado tem protocolo, histórico de movimentações e registro do técnico responsável. Tudo auditável.</span>
        </div>
      </div>
    </div>

  </div>

  <!-- BOTÃO FIXO -->
  <div class="bottom-cta">
    <button class="btn" onclick="window.Telegram?.WebApp?.close()">Entendido — Fechar</button>
  </div>

</body>
</html>
""";
    }

    public static class MiniAppDemandRequest {
        private String initData;
        private String specialtyCode;
        private String title;

        public String getInitData()          { return initData; }
        public void setInitData(String v)    { this.initData = v; }
        public String getSpecialtyCode()     { return specialtyCode; }
        public void setSpecialtyCode(String v){ this.specialtyCode = v; }
        public String getTitle()             { return title; }
        public void setTitle(String v)       { this.title = v; }
    }
}
