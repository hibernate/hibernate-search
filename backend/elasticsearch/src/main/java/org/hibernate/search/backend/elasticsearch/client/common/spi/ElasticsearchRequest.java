package org.hibernate.search.backend.elasticsearch.client.common.spi;

import java.util.Map;
import java.util.Objects;

import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.util.json.spi.JsonBody;

/** Library-neutral wire request. A null body means no HTTP entity.
 * Keep the replayable body stable through sending, signing and retries.
 */
public record ElasticsearchRequest( String method, String path, Map<String, String> parameters,
									JsonBody body, Deadline deadline) {
	public ElasticsearchRequest {
		Objects.requireNonNull( method );
		Objects.requireNonNull( path );
		parameters = java.util.Collections.unmodifiableMap( new java.util.LinkedHashMap<>( parameters ) );
	}
}
