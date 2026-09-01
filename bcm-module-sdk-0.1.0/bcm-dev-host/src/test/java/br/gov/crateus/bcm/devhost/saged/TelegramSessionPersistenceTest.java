package br.gov.crateus.bcm.devhost.saged;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.crateus.bcm.saged.application.DemandAlertScheduler;
import br.gov.crateus.bcm.saged.application.TelegramBotService;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramLinkCodeRepository;
import br.gov.crateus.bcm.saged.infrastructure.repository.TelegramSessionRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TelegramSessionPersistenceTest extends SagedIntegrationTestBase {

    @Autowired TelegramBotService botService;
    @Autowired TelegramSessionRepository sessionRepository;
    @Autowired TelegramLinkCodeRepository linkCodeRepository;
    @Autowired DemandAlertScheduler scheduler;

    // ── Link codes ────────────────────────────────────────────────────────────

    @Test
    void generateLinkCode_persistsCodeToDatabase() {
        String code = botService.generateLinkCode("111222333");

        assertThat(linkCodeRepository.findById(code)).isPresent();
        assertThat(linkCodeRepository.findById(code).get().getTelegramUserId()).isEqualTo("111222333");
        assertThat(linkCodeRepository.findById(code).get().getExpiresAt())
                .isAfter(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void consumeLinkCode_validCode_returnsTelegramUserIdAndDeletesFromDb() {
        String code = botService.generateLinkCode("444555666");

        Optional<String> result = botService.consumeLinkCode(code);

        assertThat(result).isPresent().contains("444555666");
        assertThat(linkCodeRepository.findById(code)).isEmpty();
    }

    @Test
    void consumeLinkCode_nonExistentCode_returnsEmpty() {
        Optional<String> result = botService.consumeLinkCode("000000");
        assertThat(result).isEmpty();
    }

    @Test
    void consumeLinkCode_calledTwice_secondCallReturnsEmpty() {
        String code = botService.generateLinkCode("777888999");
        botService.consumeLinkCode(code);

        Optional<String> second = botService.consumeLinkCode(code);
        assertThat(second).isEmpty();
    }

    @Test
    void generateLinkCode_sixDigitFormat() {
        String code = botService.generateLinkCode("123456789");
        assertThat(code).matches("\\d{6}");
    }

    // ── Session persistence ───────────────────────────────────────────────────

    @Test
    void session_savedAndRetrievableAfterPut() {
        String tgUser = "tg_user_session_test";

        // Simulate what handleSpecialtySelected does internally
        // We access TelegramBotService via reflection to call putSession,
        // but since it's private, we test it indirectly via DB state.
        // The DB write happens when the bot handles a specialty selection.
        // Here we verify the session repository works correctly directly.
        var session = new br.gov.crateus.bcm.saged.infrastructure.entity.TelegramSessionEntity();
        session.setTelegramUserId(tgUser);
        session.setState("WAITING_TITLE");
        session.setSpecialtyCode("HW");
        session.setSpecialtyName("Hardware");
        session.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));
        sessionRepository.save(session);

        var found = sessionRepository.findById(tgUser);
        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo("WAITING_TITLE");
        assertThat(found.get().getSpecialtyCode()).isEqualTo("HW");
    }

    @Test
    void session_deletedOnRemove() {
        String tgUser = "tg_user_delete_test";
        var session = new br.gov.crateus.bcm.saged.infrastructure.entity.TelegramSessionEntity();
        session.setTelegramUserId(tgUser);
        session.setState("WAITING_DESCRIPTION");
        session.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));
        sessionRepository.save(session);

        sessionRepository.deleteById(tgUser);

        assertThat(sessionRepository.findById(tgUser)).isEmpty();
    }

    // ── Purge scheduler ───────────────────────────────────────────────────────

    @Test
    void purgeExpiredSessions_removesExpiredButKeepsValid() {
        var expired = new br.gov.crateus.bcm.saged.infrastructure.entity.TelegramSessionEntity();
        expired.setTelegramUserId("expired_session");
        expired.setState("WAITING_TITLE");
        expired.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));
        sessionRepository.save(expired);

        var valid = new br.gov.crateus.bcm.saged.infrastructure.entity.TelegramSessionEntity();
        valid.setTelegramUserId("valid_session");
        valid.setState("WAITING_DESCRIPTION");
        valid.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(25));
        sessionRepository.save(valid);

        scheduler.purgeExpiredTelegramData();

        assertThat(sessionRepository.findById("expired_session")).isEmpty();
        assertThat(sessionRepository.findById("valid_session")).isPresent();
    }

    @Test
    void purgeExpiredLinkCodes_removesExpiredCode() {
        var expiredCode = new br.gov.crateus.bcm.saged.infrastructure.entity.TelegramLinkCodeEntity();
        expiredCode.setCode("123456");
        expiredCode.setTelegramUserId("some_user");
        expiredCode.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(15));
        linkCodeRepository.save(expiredCode);

        scheduler.purgeExpiredTelegramData();

        assertThat(linkCodeRepository.findById("123456")).isEmpty();
    }
}
