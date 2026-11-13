package org.hibernate.search.util.json.internal.spi;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.logging.impl.JsonLog;

/** Central parser constraints. Zero byte limit means unlimited parser input; transports may enforce a separate cap.
 * Limits count incoming bytes before transcoding.
 * Token limits constrain parser/tree work separately from transport body limits.
 */
@Incubating
public record JsonParsingOptions(   int maxDepth, int maxStringLength, int maxNameLength, int maxNumberLength,
									int maxInputChunkSize, long maxInputBytes) {
	/** Default maximum bytes accepted by one parser feed. */
	public static final int DEFAULT_MAX_INPUT_CHUNK_SIZE = 8192;

	public static final JsonParsingOptions DEFAULT =
			new JsonParsingOptions( 1000, 20_000_000, 50_000, 1000, DEFAULT_MAX_INPUT_CHUNK_SIZE, 0 );

	public JsonParsingOptions {
		if ( maxDepth <= 0
				|| maxStringLength <= 0 || maxNameLength <= 0 || maxNumberLength <= 0
				|| maxInputChunkSize <= 0 || maxInputBytes < 0 ) {
			throw JsonLog.INSTANCE.invalidParsingConstraints();
		}
	}

	public JsonParsingOptions withMaxInputBytes(long limit) {
		return new JsonParsingOptions( maxDepth, maxStringLength, maxNameLength, maxNumberLength, maxInputChunkSize, limit );
	}
}
