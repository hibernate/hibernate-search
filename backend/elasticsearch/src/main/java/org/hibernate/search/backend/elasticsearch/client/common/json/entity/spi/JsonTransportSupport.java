package org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi;

/** Default bounded buffer and decoded response policies shared by HTTP transports. */
public final class JsonTransportSupport {
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	public static final int DEFAULT_INPUT_CAPACITY = 8192;
	public static final long DEFAULT_LIMIT = 100L * 1024 * 1024;

	private JsonTransportSupport() {
	}
}
