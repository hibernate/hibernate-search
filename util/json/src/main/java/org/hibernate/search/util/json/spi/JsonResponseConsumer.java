package org.hibernate.search.util.json.spi;

import java.io.InputStream;
import java.nio.ByteBuffer;

/** One response attempt. Implementations are supplied by Search, not by transports.
 * Feed decoded bytes synchronously; caller storage is not retained. Complete before obtaining
 * the result. Deferred parse failures allow HTTP clients to make status-based retry decisions.
 * Close on every terminal path, including abandoned attempts. Completion and close must be idempotent.
 * After successful completion, result and diagnostic replay remain accessible after close;
 * closing releases active parsing resources, not the completed result.
 */
@org.hibernate.search.util.common.annotation.Incubating
public interface JsonResponseConsumer<T> extends AutoCloseable {
	void feed(ByteBuffer input);

	void complete();

	T result();

	void failedParsing(RuntimeException failure);

	void throwIfFailed();

	InputStream openDiagnosticStream();

	/**
	 * Cumulative bytes delivered after content decoding and before charset conversion,
	 * including a batch rejected for exceeding the configured limit.
	 */
	long inputBytes();

	@Override
	void close();
}
