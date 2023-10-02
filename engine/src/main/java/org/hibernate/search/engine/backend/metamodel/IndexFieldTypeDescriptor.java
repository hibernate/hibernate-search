/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.metamodel;

import java.util.Set;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;

/**
 * The type of a field in the index,
 * exposing its various capabilities.
 *
 * @see IndexFieldDescriptor#type()
 * @see IndexValueFieldTypeDescriptor
 * @see IndexObjectFieldTypeDescriptor
 */
public interface IndexFieldTypeDescriptor {

	/**
	 * @return An (unmodifiable) set of strings
	 * representing the {@link IndexFieldTraits field traits}
	 * enabled for fields of this type.
	 */
	Set<String> traits();

}
