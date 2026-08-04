package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.DemandViewEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandViewRepository extends JpaRepository<DemandViewEntity, UUID> {

    List<DemandViewEntity> findByDemandIdOrderByViewedAtAsc(UUID demandId);

    Optional<DemandViewEntity> findByDemandIdAndViewerUserId(UUID demandId, UUID viewerUserId);

    boolean existsByDemandIdAndViewerUserId(UUID demandId, UUID viewerUserId);
}
