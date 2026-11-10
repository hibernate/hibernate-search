package org.hibernate.search.backend.elasticsearch.client.common.json.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonValidation;
import org.hibernate.search.util.json.spi.JsonEmptyInputException;

import tools.jackson.databind.JsonNode;

/** Per-attempt result carrier. Parse errors are deferred until the client has decided whether to retry. */
public final class JsonResponseBody implements org.hibernate.search.util.json.spi.JsonResponseConsumer<JsonNode> {
	/** Default response input capacity advertised/allocated by REST adapters. */
	public static final int DEFAULT_INPUT_CAPACITY = 8192;

	private static final int DIAGNOSTIC_PREFIX_LIMIT = 8192;

	public static final long DEFAULT_LIMIT = 100L * 1024 * 1024;
	private JsonParserSession parser;
	private final Charset charset;
	private final JsonValidation validation;
	private final long limit;
	private byte[] diagnosticPrefix = new byte[DIAGNOSTIC_PREFIX_LIMIT];
	private int prefixLength;
	private long inputBytes;
	private JsonNode tree;
	private RuntimeException failure;
	private boolean completed;

	public JsonResponseBody(Charset charset, long limit) {
		this( charset, limit, JsonValidation.systemDefault() );
	}

	public JsonResponseBody(Charset charset, long limit, JsonValidation validation) {
		this.validation = Objects.requireNonNull( validation );
		this.charset = charset;
		this.limit = limit;
		parser = new JsonParsing().createParser( charset );
	}

	public void feed(ByteBuffer input) {
		if ( completed ) {
			throw ElasticsearchClientLog.INSTANCE.responseAlreadyComplete();
		}
		int count = input.remaining();
		inputBytes += count;
		if ( limit > 0 && inputBytes > limit ) {
			throw ElasticsearchClientLog.INSTANCE.responseInputLimitExceeded( limit );
		}
		int capture = Math.min( count, diagnosticPrefix.length - prefixLength );
		ByteBuffer prefix = input.duplicate();
		prefix.get( diagnosticPrefix, prefixLength, capture );
		prefixLength += capture;
		if ( failure == null ) {
			try {
				while ( input.hasRemaining() ) {
					parser.feed( input );
				}
			}
			catch (RuntimeException e) {
				failedParsing( e );
			}
		}
		// Drain malformed responses as well so status-based failover and connection reuse still work.
		input.position( input.limit() );
	}

	public void failedParsing(RuntimeException cause) {
		if ( failure == null ) {
			failure = cause;
		}
		closeParser();
	}

	public void complete() {
		if ( completed ) {
			return;
		}
		completed = true;
		if ( failure == null ) {
			try {
				tree = parser.endOfInput();
			}
			catch (JsonEmptyInputException e) {
				// Empty/whitespace HTTP bodies are absent; JSON null remains a parsed root.
			}
			catch (RuntimeException e) {
				failure = e;
			}
		}
		closeParser();
		if ( failure == null ) {
			diagnosticPrefix = null;
		}
		else {
			diagnosticPrefix = Arrays.copyOf( diagnosticPrefix, prefixLength );
		}
	}

	@Override
	public JsonNode result() {
		return tree();
	}

	public JsonNode tree() {
		if ( !completed ) {
			throw ElasticsearchClientLog.INSTANCE.responseIncomplete();
		}
		throwIfFailed();
		return tree;
	}

	public void throwIfFailed() {
		if ( failure != null ) {
			throw failure;
		}
	}

	/** Diagnostic replay is normalized UTF-8 JSON, or an explicitly truncated source prefix on failure. */
	public InputStream openDiagnosticStream() {
		if ( !completed ) {
			throw ElasticsearchClientLog.INSTANCE.responseIncomplete();
		}
		if ( failure != null ) {
			if ( diagnosticPrefix == null ) {
				return InputStream.nullInputStream();
			}
			String prefix = new String( diagnosticPrefix, charset );
			if ( inputBytes > prefixLength ) {
				prefix += "\n[response diagnostic truncated after " + DIAGNOSTIC_PREFIX_LIMIT + " source bytes]";
			}
			return new ByteArrayInputStream( prefix.getBytes( StandardCharsets.UTF_8 ) );
		}
		return tree == null ? InputStream.nullInputStream() : JsonBodies.ofInternal( tree, validation ).openStream();
	}

	public long inputBytes() {
		return inputBytes;
	}

	private void closeParser() {
		if ( parser != null ) {
			parser.close();
			parser = null;
		}
	}

	@Override
	public void close() {
		closeParser();
		if ( !completed ) {
			diagnosticPrefix = null;
			tree = null;
			completed = true;
			if ( failure == null ) {
				failure = ElasticsearchClientLog.INSTANCE.responseConsumptionCancelled();
			}
		}
	}
}
