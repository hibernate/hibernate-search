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
 * A marker annotation for the field containing a map of extra properties.
 * <p>
 * Used by the Gson type adapter annotation processor to generate reflection-free adapters.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.FIELD })
public @interface SerializeExtraProperties {

}
