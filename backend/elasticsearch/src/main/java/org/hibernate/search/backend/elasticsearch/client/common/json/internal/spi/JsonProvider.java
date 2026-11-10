package org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.internal.spi.JsonCodecRegistry;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

/** Fixed Jackson parsing, bounded output and generated-codec services supplied to client extensions. */
@Incubating
public final class JsonProvider {
	private final JsonCodecService codec, codecNoSerializeNulls;
	private final JsonLogHelper logHelper;

	public JsonProvider(JsonCodecRegistry registry, boolean logPrettyPrinting) {
		this( registry, logPrettyPrinting, JsonValidation.systemDefault() );
	}

	public JsonProvider(JsonCodecRegistry registry, boolean logPrettyPrinting, JsonValidation validation) {
		codec = new JsonCodecService( registry, true, false, validation );
		codecNoSerializeNulls = new JsonCodecService( registry, false, false, validation );
		logHelper = JsonLogHelper.create( new JsonCodecService( registry, false, logPrettyPrinting, validation ),
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
