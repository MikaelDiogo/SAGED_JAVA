package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterEntity;
import br.gov.crateus.bcm.saged.infrastructure.entity.TelegramRequesterStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelegramRequesterRepository extends JpaRepository<TelegramRequesterEntity, UUID> {

    Optional<TelegramRequesterEntity> findByTelegramChatId(String telegramChatId);

    Optional<TelegramRequesterEntity> findByTelegramChatIdAndStatus(String telegramChatId, TelegramRequesterStatus status);

    boolean existsByTelegramChatId(String telegramChatId);

    List<TelegramRequesterEntity> findByDepartmentId(UUID departmentId);
}
