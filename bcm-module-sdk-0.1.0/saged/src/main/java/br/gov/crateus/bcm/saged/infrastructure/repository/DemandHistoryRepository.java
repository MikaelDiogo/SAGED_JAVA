package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.DemandHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemandHistoryRepository extends JpaRepository<DemandHistoryEntity, UUID> {

    @Query("SELECT h FROM DemandHistoryEntity h WHERE h.demand.id = :demandId ORDER BY h.createdAt ASC")
    List<DemandHistoryEntity> findByDemandIdOrdered(@Param("demandId") UUID demandId);
}
