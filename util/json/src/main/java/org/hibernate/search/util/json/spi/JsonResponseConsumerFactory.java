package org.hibernate.search.util.json.spi;

import java.nio.charset.Charset;

/** Shareable factory: create an independent consumer for each response attempt. */
@FunctionalInterface
@org.hibernate.search.util.common.annotation.Incubating
public interface JsonResponseConsumerFactory<T> {
	/**
	 * Create a consumer that enforces the cumulative limit on bytes after content decoding (such as gzip),
	 * before charset conversion, including while draining after a deferred parsing failure.
	 * @param charset The response charset.
	 * @param decodedByteLimit Maximum decoded bytes, or zero for unlimited; must be non-negative.
	 * @return An independent response consumer.
	 */
	JsonResponseConsumer<T> create(Charset charset, long decodedByteLimit);
}
