package org.hibernate.search.util.json.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.impl.JsonBodyOutput;

/**
 * Replayable UTF-8 JSON output source. Encoded payloads need not be copied or retained.
 * Implementations must provide stable bytes through sending, signing and retries.
 * Sources may be shared; their independent cursors and streams are single-consumer state machines.
 */
@Incubating
public interface JsonBody {
	JsonWriteCursor openCursor();

	/**
	 * Stable length for this source/body attempt, or {@code -1} when unknown.
	 * Completing a cursor or signing read must not change this value.
	 */
	default long contentLength() {
		return -1;
	}

	/** Write without flushing or closing the caller-owned destination. */
	default void writeTo(OutputStream destination) throws IOException {
		JsonBodyOutput.writeTo( this, destination );
	}

	/**
	 * Open an independent cursor-backed stream, suitable for synchronous signing and replay.
	 * Reads use bounded 8 KiB storage. Closing the stream cancels its cursor.
	 */
	default InputStream openStream() {
		return JsonBodyOutput.openStream( this );
	}
}
