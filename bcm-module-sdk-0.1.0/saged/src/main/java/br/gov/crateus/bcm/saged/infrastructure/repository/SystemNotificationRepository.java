package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.SystemNotificationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemNotificationRepository extends JpaRepository<SystemNotificationEntity, UUID> {

    @Query("""
            SELECT n FROM SystemNotificationEntity n
            WHERE n.recipientUserId = :userId
               OR (n.recipientUserId IS NULL AND (:deptId IS NULL OR n.departmentId IS NULL OR n.departmentId = :deptId))
            ORDER BY n.createdAt DESC
            """)
    List<SystemNotificationEntity> findForUser(@Param("userId") UUID userId, @Param("deptId") UUID deptId);

    @Query("""
            SELECT COUNT(n) FROM SystemNotificationEntity n
            WHERE (n.recipientUserId = :userId
               OR (n.recipientUserId IS NULL AND (:deptId IS NULL OR n.departmentId IS NULL OR n.departmentId = :deptId)))
              AND n.read = false
            """)
    long countUnreadForUser(@Param("userId") UUID userId, @Param("deptId") UUID deptId);

    boolean existsByDemandIdAndType(UUID demandId, String type);
}
