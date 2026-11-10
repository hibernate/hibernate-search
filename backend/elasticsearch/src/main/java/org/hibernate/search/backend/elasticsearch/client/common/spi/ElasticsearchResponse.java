package org.hibernate.search.backend.elasticsearch.client.common.spi;

/**
 * HTTP metadata and the exact opaque result produced by the supplied response consumer.
 * A client constructs this response without inspecting or converting the result.
 * JSON consumer results can implement {@link ElasticsearchResponseBody}; access their JSON with
 * {@code response.result().body(handler)}.
 * @param <T> The supplied consumer's result type.
 */
public class ElasticsearchResponse<T> {
	private final String hostAndPort;
	private final int statusCode;
	private final String statusMessage;
	private final T result;

	public ElasticsearchResponse(String hostAndPort, int statusCode, String statusMessage, T result) {
		this.hostAndPort = hostAndPort;
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.result = result;
	}

	public String hostAndPort() {
		return hostAndPort;
	}

	public int statusCode() {
		return statusCode;
	}

	public String statusMessage() {
		return statusMessage;
	}

	public T result() {
		return result;
	}
}
