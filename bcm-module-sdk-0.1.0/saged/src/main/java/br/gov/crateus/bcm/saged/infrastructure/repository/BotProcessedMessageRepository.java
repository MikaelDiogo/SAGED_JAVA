package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.BotProcessedMessageEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BotProcessedMessageRepository extends JpaRepository<BotProcessedMessageEntity, UUID> {
    boolean existsByProviderAndExternalMessageId(String provider, String externalMessageId);
}
