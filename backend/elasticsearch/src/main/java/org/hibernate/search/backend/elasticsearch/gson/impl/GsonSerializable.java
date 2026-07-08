/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.gson.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class for compile-time generation of a reflection-free Gson TypeAdapter.
 * <p>
 * The annotation processor will generate a {@code TypeAdapterFactory} that reads/writes
 * all fields via getters/setters, using {@code @SerializedName} for JSON key mapping
 * and {@code @SerializeExtraProperties} for unknown-key collection.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GsonSerializable {

}
