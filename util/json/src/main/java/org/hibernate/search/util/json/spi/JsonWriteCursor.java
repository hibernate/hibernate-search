package org.hibernate.search.util.json.spi;

import java.nio.ByteBuffer;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A single-consumer UTF-8 output traversal. Each cursor has independent replay state.
 * No destination buffer is retained: callers own emitted storage and may reuse it after their sink consumes it.
 * A sink accepting only part of a filled buffer must retain its unread bytes before requesting more.
 * No executor, thread hop or sink scheduling is imposed.
 */
@Incubating
public interface JsonWriteCursor extends AutoCloseable {
	/**
	 * Fill only the destination's remaining region and advance its position by the returned byte count.
	 * A destination with no remaining capacity makes no progress. The limit is never changed.
	 * Read-only destinations are rejected before changing cursor state.
	 * @throws IllegalStateException if closed or cancelled
	 */
	int write(ByteBuffer destination);

	/** Whether all output has been emitted. Completion releases traversal state. */
	boolean isComplete();

	/** Cancel this traversal and release its references. Equivalent to closing it. */
	default void cancel() {
		close();
	}

	/** Idempotently release traversal state. Does not close a caller's destination. */
	@Override
	void close();
}
