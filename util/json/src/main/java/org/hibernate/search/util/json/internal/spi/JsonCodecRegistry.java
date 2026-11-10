package org.hibernate.search.util.json.internal.spi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.logging.impl.JsonLog;

import tools.jackson.databind.JsonNode;

/** Immutable explicit registrations. Unknown classes fail; there is no reflective fallback. */
@Incubating
public final class JsonCodecRegistry {
	private final Map<Class<?>, JsonCodec<?>> codecs;

	public JsonCodecRegistry(Map<Class<?>, Function<JsonCodecRegistry, JsonCodec<?>>> factories) {
		Map<Class<?>, JsonCodec<?>> registrations = new LinkedHashMap<>();
		codecs = Collections.unmodifiableMap( registrations );
		for ( var entry : factories.entrySet() ) {
			registrations.put( entry.getKey(), entry.getValue().apply( this ) );
		}
	}

	@SuppressWarnings("unchecked")
	public <T> JsonCodec<T> get(Class<T> type) {
		JsonCodec<?> codec = codecs.get( type );
		if ( codec == null ) {
			throw JsonLog.INSTANCE.unknownCodec( type.getName() );
		}
		return (JsonCodec<T>) codec;
	}

	/** Deferred direct codec lookup allows recursive model graphs without reflective mapping. */
	public <T> JsonCodec<T> reference(Class<T> type) {
		return new JsonCodec<>() {
			public T decode(JsonNode value) {
				return get( type ).decode( value );
			}

			public JsonNode encode(T value, boolean serializeNulls) {
				return get( type ).encode( value, serializeNulls );
			}
		};
	}
}
