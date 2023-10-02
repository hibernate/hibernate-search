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
 * Defines several sortable fields.
 *
 * @author Gunnar Morling
 * @deprecated See the deprecation note on {@link SortableField}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Deprecated
public @interface SortableFields {

	/**
	 * @return the sortable fields of the annotated property
	 */
	SortableField[] value();
}
