package org.hibernate.search.backend.elasticsearch.client.common.spi;

import org.hibernate.search.util.json.JsonHandler;

/**
 * A JSON response value supplied by the backend's response consumer.
 * Transports pass this value through unchanged and need no JSON library.
 * Returned values are borrowed and must be treated as read-only.
 */
public interface ElasticsearchResponseBody {
	/** @return The body in the handler's representation, or null for an absent body. */
	<T> T body(JsonHandler<T> handler);
}
