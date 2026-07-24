package br.gov.crateus.bcm.sdk.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Minimum auditable fields every BDM business entity must expose.
 * BCM JPA entities implement this via {@code BaseAuditableEntity}.
 */
public interface AuditableRecord {

	UUID getId();

	OffsetDateTime getCreatedAt();

	OffsetDateTime getUpdatedAt();

	String getCreatedBy();

	String getUpdatedBy();

	UUID getOrgId();

	String getSource();

	String getSensitivity();

	String getLifecycleStatus();

	long getVersion();
}
