package org.hibernate.search.util.json.internal.spi;

import java.nio.ByteBuffer;

import org.hibernate.search.util.common.annotation.Incubating;

import tools.jackson.databind.JsonNode;

/** Single-consumer incremental JSON parser. It owns no executor or input queue.
 * Each feed consumes at most the configured chunk size, advances the caller's position by the accepted slice on success,
 * and drains available tokens before returning. The caller owns storage again after return, including on failure (the position is unspecified on failure).
 * Call feed repeatedly while the buffer has remaining bytes. Do not mutate storage during a call.
 * A root is returned only by endOfInput, after rejecting empty, truncated and trailing input.
 * Transports interpret absent HTTP bodies independently. Close cancels and releases partial tree/token/buffer state.
 * Parser token completion/tree construction can consume significant CPU; this SPI promises no latency bound.
 */
@Incubating
public interface JsonParserSession extends AutoCloseable {
	void feed(ByteBuffer input);

	JsonNode endOfInput();

	long inputBytes();

	@Override
	void close();
}
