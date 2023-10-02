/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.metamodel;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The type of an "object" field in the index,
 * exposing its various capabilities.
 *
 * @see IndexObjectFieldDescriptor
 */
public interface IndexObjectFieldTypeDescriptor extends IndexFieldTypeDescriptor {

	/**
	 * @return {@code true} if this object field is represented internally as a nested document,
	 * enabling features such as the {@link SearchPredicateFactory#nested(String) nested predicate}.
	 */
	boolean nested();

}
