package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramContactEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramContactRepository extends JpaRepository<TelegramContactEntity, UUID> {
    Optional<TelegramContactEntity> findByTelegramUserId(String telegramUserId);
}
