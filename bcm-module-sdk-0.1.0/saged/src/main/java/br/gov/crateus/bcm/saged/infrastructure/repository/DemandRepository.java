package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.domain.DemandStatus;
import br.gov.crateus.bcm.saged.infrastructure.entity.DemandEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemandRepository extends JpaRepository<DemandEntity, UUID>, JpaSpecificationExecutor<DemandEntity> {
    Optional<DemandEntity> findByProtocol(String protocol);
    boolean existsByProtocol(String protocol);
    List<DemandEntity> findByRequesterUserId(UUID requesterUserId);
    List<DemandEntity> findByDepartmentId(UUID departmentId);
    List<DemandEntity> findByStatus(DemandStatus status);
    List<DemandEntity> findByDepartmentIdAndStatus(UUID departmentId, DemandStatus status);
    List<DemandEntity> findByAssigneeUserId(UUID assigneeUserId);

    @Query(value = "SELECT COUNT(*) FROM saged.demands WHERE specialty_id = :specialtyId AND EXTRACT(YEAR FROM created_at) = :year", nativeQuery = true)
    long countBySpecialtyIdAndYear(@Param("specialtyId") UUID specialtyId, @Param("year") int year);

    @Query(value = "SELECT COUNT(*) FROM saged.demands WHERE department_id = :deptId AND specialty_id = :specialtyId AND EXTRACT(YEAR FROM created_at) = :year", nativeQuery = true)
    long countByDepartmentIdAndSpecialtyIdAndYear(@Param("deptId") UUID deptId, @Param("specialtyId") UUID specialtyId, @Param("year") int year);

    @Query("SELECT d FROM DemandEntity d WHERE d.specialty.code IN :specialtyCodes")
    List<DemandEntity> findBySpecialtyCodeIn(@Param("specialtyCodes") List<String> specialtyCodes);

    @Query(value = """
            SELECT d.* FROM saged.demands d
            WHERE d.assignee_user_id IS NULL
              AND d.status IN ('TODO','IN_PROGRESS')
              AND d.created_at < :threshold
              AND NOT EXISTS (
                  SELECT 1 FROM saged.system_notifications n
                  WHERE n.demand_id = d.id AND n.type = :type
              )
            """, nativeQuery = true)
    List<DemandEntity> findUnattendedWithoutAlert(@Param("threshold") OffsetDateTime threshold,
                                                   @Param("type") String type);
}
