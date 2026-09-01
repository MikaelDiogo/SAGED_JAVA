package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramLinkCodeEntity;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TelegramLinkCodeRepository extends JpaRepository<TelegramLinkCodeEntity, String> {

    @Modifying
    @Query("DELETE FROM TelegramLinkCodeEntity c WHERE c.expiresAt < :now")
    void deleteExpired(OffsetDateTime now);
}
