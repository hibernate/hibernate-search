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
 * Specifies a custom Gson {@code TypeAdapter} to use for serializing/deserializing a field,
 * instead of using the default adapter resolved from the field's type.
 * <p>
 * The referenced class must extend {@code com.google.gson.TypeAdapter} and have a no-arg constructor.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface GsonTypeAdapter {
	/**
	 * @return The custom TypeAdapter class to use for this field.
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends com.google.gson.TypeAdapter> value();
}
