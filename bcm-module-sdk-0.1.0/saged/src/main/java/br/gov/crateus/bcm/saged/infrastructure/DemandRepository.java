package br.gov.crateus.bcm.saged.infrastructure;

import br.gov.crateus.bcm.saged.domain.DemandStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandRepository extends JpaRepository<DemandEntity, UUID> {

    Optional<DemandEntity> findByProtocol(String protocol);

    boolean existsByProtocol(String protocol);

    List<DemandEntity> findByDepartmentId(UUID departmentId);

    List<DemandEntity> findBySpecialtyId(UUID specialtyId);

    List<DemandEntity> findByStatus(DemandStatus status);

    List<DemandEntity> findByDepartmentIdAndStatus(UUID departmentId, DemandStatus status);

    List<DemandEntity> findByAssigneeUserId(UUID assigneeUserId);
}
