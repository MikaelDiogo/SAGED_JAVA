package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramSessionEntity;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TelegramSessionRepository extends JpaRepository<TelegramSessionEntity, String> {

    @Modifying
    @Query("DELETE FROM TelegramSessionEntity s WHERE s.expiresAt < :now")
    void deleteExpired(OffsetDateTime now);
}
