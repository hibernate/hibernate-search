package org.hibernate.search.backend.elasticsearch.json.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class for compile-time generation of a reflection-free Search JSON codec.
 * <p>
 * The annotation processor will generate a {@code JsonCodec} that reads/writes
 * all fields via getters/setters, using {@code @JsonName} for JSON key mapping
 * and {@code @SerializeExtraProperties} for unknown-key collection.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface JsonSerializable {

}
