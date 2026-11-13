package org.hibernate.search.util.json.internal.spi;

import java.nio.charset.Charset;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.impl.JacksonJsonParserFactory;

import tools.jackson.databind.JsonNode;

/** Shareable parser factory with immutable constraints and Jackson-native signatures. */
@Incubating
public final class JsonParsing {
	private final JacksonJsonParserFactory implementation;

	public JsonParsing() {
		this( JsonParsingOptions.DEFAULT );
	}

	public JsonParsing(JsonParsingOptions options) {
		implementation = new JacksonJsonParserFactory( options );
	}

	public JsonParserSession createParser() {
		return implementation.createParser();
	}

	/** Parse an explicitly selected source charset, transcoding incrementally with bounded buffers. */
	public JsonParserSession createParser(Charset charset) {
		return implementation.createParser( charset );
	}

	/** Parse an already materialized string without creating a secondary encoded body. */
	public JsonNode parse(String json) {
		return implementation.parse( json );
	}
}
