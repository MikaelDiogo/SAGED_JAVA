package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.SystemNotificationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemNotificationRepository extends JpaRepository<SystemNotificationEntity, UUID> {

    @Query("SELECT n FROM SystemNotificationEntity n WHERE n.recipientUserId = :userId OR n.recipientUserId IS NULL ORDER BY n.createdAt DESC")
    List<SystemNotificationEntity> findForUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(n) FROM SystemNotificationEntity n WHERE (n.recipientUserId = :userId OR n.recipientUserId IS NULL) AND n.read = false")
    long countUnreadForUser(@Param("userId") UUID userId);

    boolean existsByDemandIdAndType(UUID demandId, String type);
}
