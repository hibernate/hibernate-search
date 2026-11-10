package org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.internal.spi.JsonCodecRegistry;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

/** Backend-owned parsing, bounded output and generated-codec services. */
@Incubating
public final class JsonProvider {
	private final JsonCodecService codec, codecNoSerializeNulls;
	private final JsonLogHelper logHelper;

	public JsonProvider(JsonCodecRegistry registry, boolean logPrettyPrinting) {
		this( registry, logPrettyPrinting, JsonValidation.systemDefault() );
	}

	public JsonProvider(JsonCodecRegistry registry, boolean logPrettyPrinting, JsonValidation validation) {
		this( registry, logPrettyPrinting, validation, new JsonParsing() );
	}

	public JsonProvider(JsonCodecRegistry registry, boolean logPrettyPrinting, JsonValidation validation, JsonParsing parsing) {
		codec = new JsonCodecService( registry, true, false, validation, parsing );
		codecNoSerializeNulls = new JsonCodecService( registry, false, false, validation, parsing );
		logHelper = JsonLogHelper.create( new JsonCodecService( registry, false, logPrettyPrinting, validation, parsing ),
				logPrettyPrinting );
	}

	public JsonCodecService getCodec() {
		return codec;
	}

	public JsonCodecService getCodecNoSerializeNulls() {
		return codecNoSerializeNulls;
	}

	public JsonLogHelper getLogHelper() {
		return logHelper;
	}
}
