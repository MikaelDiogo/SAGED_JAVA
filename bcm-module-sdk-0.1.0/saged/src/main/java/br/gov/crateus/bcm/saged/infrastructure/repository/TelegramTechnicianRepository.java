package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramTechnicianEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramTechnicianRepository extends JpaRepository<TelegramTechnicianEntity, UUID> {

    Optional<TelegramTechnicianEntity> findByTelegramUserId(String telegramUserId);

    Optional<TelegramTechnicianEntity> findByKeycloakUserId(UUID keycloakUserId);

    boolean existsByTelegramUserId(String telegramUserId);

    boolean existsByKeycloakUserId(UUID keycloakUserId);
}
