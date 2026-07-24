package br.gov.crateus.bcm.saged.infrastructure.repository;

import br.gov.crateus.bcm.saged.infrastructure.entity.AssetEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<AssetEntity, UUID> {
    Optional<AssetEntity> findByAssetTag(String assetTag);
    boolean existsByAssetTag(String assetTag);
}
