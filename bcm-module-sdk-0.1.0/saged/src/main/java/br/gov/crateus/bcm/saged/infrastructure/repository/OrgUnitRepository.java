package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.OrgUnitEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgUnitRepository extends JpaRepository<OrgUnitEntity, UUID> {
}
