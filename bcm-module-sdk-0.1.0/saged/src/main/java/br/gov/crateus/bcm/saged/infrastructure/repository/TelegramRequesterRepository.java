package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramRequesterRepository extends JpaRepository<TelegramRequesterEntity, UUID> {
    Optional<TelegramRequesterEntity> findByTelegramChatIdAndActiveTrue(String telegramChatId);
}
