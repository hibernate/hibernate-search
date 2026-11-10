package org.hibernate.search.util.json.internal.spi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.json.impl.JacksonJsonBodyWriter;
import org.hibernate.search.util.json.impl.JsonBodyOutput;
import org.hibernate.search.util.json.impl.NdjsonBody;
import org.hibernate.search.util.json.impl.Utf8JsonWriteCursor;
import org.hibernate.search.util.json.logging.impl.JsonLog;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import tools.jackson.databind.JsonNode;

/**
 * Jackson-native output factories. Blocking output uses the native writer by default;
 * cursors and input streams retain independent cursor-backed traversal.
 */
@Incubating
public final class JsonBodies {

	/** Byte buffer capacity used by cursor-backed output adapters. */
	public static final int DEFAULT_BUFFER_SIZE = 8192;

	/** Maximum bytes inspected once to determine a small body's content length. */
	public static final int DEFAULT_CONTENT_LENGTH_PROBE_LIMIT = 8192;

	private JsonBodies() {
	}

	/**
	 * Create a replayable source and probe at most 8 KiB for a small body's length.
	 * Strings/names escape JSON controls, HTML-sensitive characters and U+2028/U+2029 using
	 * lowercase hex. Valid surrogate pairs use UTF-8; isolated surrogates use Unicode escapes.
	 * Exact numeric values and object insertion order are preserved. No newline or HTTP framing is added.
	 */
	public static JsonBody of(JsonNode root) {
		JsonTreeValidation.validateExternal( root );
		return bodyFromInternal( root, JsonValidation.systemDefault() );
	}

	/**
	 * Every retained part ends with one newline; probe the complete framed body once.
	 */
	public static JsonBody ndjson(List<? extends JsonNode> parts) {
		Contracts.assertNotNull( parts, "parts" );
		for ( JsonNode part : parts ) {
			Contracts.assertNotNull( part, "NDJSON part" );
			JsonTreeValidation.validateExternal( part );
		}
		return framedFromInternal( parts, JsonValidation.systemDefault() );
	}

	/**
	 * Open an independent traversal without any preliminary length probe.
	 */
	public static JsonWriteCursor openCursor(JsonNode root) {
		JsonTreeValidation.validateExternal( root );
		return new Utf8JsonWriteCursor( root );
	}

	/**
	 * Internal route: caller guarantees a stable ordinary acyclic tree constructed/parsed by Search
	 * or already validated at its external boundary. Arbitrary application trees must use {@link #of(JsonNode)}.
	 * Debug validation is assurance only; it does not establish safety of arbitrary mutable trees.
	 */
	public static JsonBody ofInternal(JsonNode root) {
		return ofInternal( root, JsonValidation.systemDefault() );
	}

	/** Same internal caller contract, with an immutable context policy. */
	public static JsonBody ofInternal(JsonNode root, JsonValidation validation) {
		Objects.requireNonNull( validation );
		Objects.requireNonNull( root );
		validation.validateInternal( root );
		return bodyFromInternal( root, validation );
	}

	/** Same internal caller contract as {@link #ofInternal(JsonNode)}, without a length probe. */
	public static JsonWriteCursor openCursorInternal(JsonNode root) {
		return openCursorInternal( root, JsonValidation.systemDefault() );
	}

	/** Same internal caller contract, with an immutable context policy. */
	public static JsonWriteCursor openCursorInternal(JsonNode root, JsonValidation validation) {
		Objects.requireNonNull( validation );
		Objects.requireNonNull( root );
		validation.validateInternal( root );
		return new Utf8JsonWriteCursor( root, validation );
	}

	/** Same internal caller contract as {@link #ofInternal(JsonNode)} for every part and the retained list. */
	public static JsonBody ndjsonInternal(List<? extends JsonNode> parts) {
		return ndjsonInternal( parts, JsonValidation.systemDefault() );
	}

	/** Same internal caller contract, with an immutable context policy. */
	public static JsonBody ndjsonInternal(List<? extends JsonNode> parts, JsonValidation validation) {
		Objects.requireNonNull( validation );
		Contracts.assertNotNull( parts, "parts" );
		for ( JsonNode part : parts ) {
			Contracts.assertNotNull( part, "NDJSON part" );
			validation.validateInternal( part );
		}
		return framedFromInternal( parts, validation );
	}

	private static JsonBody bodyFromInternal(JsonNode root, JsonValidation validation) {
		return withContentLengthProbe( new InternalJsonBody() {
			@Override
			public JsonWriteCursor openCursor() {
				return new Utf8JsonWriteCursor( root, validation );
			}

			@Override
			public void writeTo(OutputStream destination, JsonWriteStrategy strategy) throws IOException {
				if ( strategy == JsonWriteStrategy.NATIVE ) {
					JacksonJsonBodyWriter.writeInternal( root, Objects.requireNonNull( destination ) );
				}
				else {
					InternalJsonBody.super.writeTo( destination, strategy );
				}
			}
		}, DEFAULT_CONTENT_LENGTH_PROBE_LIMIT );
	}

	private static JsonBody framedFromInternal(List<? extends JsonNode> parts, JsonValidation validation) {
		return withContentLengthProbe( new NdjsonBody( parts, validation ), DEFAULT_CONTENT_LENGTH_PROBE_LIMIT );
	}

	/**
	 * Probe at most {@code maximumBytes}, once, without retaining the probe's encoded bytes.
	 * An incomplete probe records {@code -1} permanently; subsequent traversal cannot change it.
	 * Use this on independently framed sources as well as individual JSON values.
	 */
	public static JsonBody withContentLengthProbe(JsonBody source, int maximumBytes) {
		Objects.requireNonNull( source );
		if ( maximumBytes < 0 ) {
			throw JsonLog.INSTANCE.negativeProbeLimit();
		}
		long length = JsonBodyOutput.probeLength( source, maximumBytes );
		return new InternalJsonBody() {

			@Override
			public JsonWriteCursor openCursor() {
				return source.openCursor();
			}

			@Override
			public void writeTo(OutputStream destination, JsonWriteStrategy strategy) throws IOException {
				JsonWriting.writeTo( source, destination, strategy );
			}

			@Override
			public long contentLength() {
				return length;
			}
		};
	}
}
