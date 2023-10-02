/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation to allow the declaration of multiple {@code @Facet} annotations.
 *
 * @author Hardy Ferentschik
 * @hsearch.experimental : This feature is experimental
 * @deprecated See the deprecation note on {@link Facet}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Deprecated
public @interface Facets {
	/**
	 * @return an array if {@code Facet} instances
	 */
	Facet[] value();
}
