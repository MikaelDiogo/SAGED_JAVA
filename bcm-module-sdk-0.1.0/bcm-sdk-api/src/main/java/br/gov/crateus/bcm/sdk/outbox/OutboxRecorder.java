package br.gov.crateus.bcm.sdk.outbox;

/**
 * Contract for recording domain events in the transactional outbox.
 * Implementations join the caller's business transaction (MANDATORY in BCM).
 */
public interface OutboxRecorder {

	/**
	 * @param aggregateType logical aggregate name (e.g. {@code Neighborhood})
	 * @param aggregateId   aggregate identifier as string
	 * @param eventType     event name (e.g. {@code NeighborhoodCreated})
	 * @param payload       JSON-serializable payload
	 */
	void record(String aggregateType, String aggregateId, String eventType, Object payload);
}
