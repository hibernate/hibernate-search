package org.hibernate.search.backend.elasticsearch.json.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.util.json.internal.spi.JsonCodec;

/**
 * Specifies a custom Search JSON codec to use for serializing/deserializing a field,
 * instead of using the default adapter resolved from the field's type.
 * <p>
 * The referenced class must extend {@code org.hibernate.search.util.json.internal.spi.JsonCodec} and have a no-arg constructor.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface JsonFieldCodec {
	/**
	 * @return The custom JsonCodec class to use for this field.
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends JsonCodec> value();
}
