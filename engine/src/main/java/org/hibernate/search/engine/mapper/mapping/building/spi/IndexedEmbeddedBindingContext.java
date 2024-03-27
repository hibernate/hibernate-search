/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Collection;

import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;

/**
 * The binding context associated to a specific non-root node in the entity tree.
 *
 * @see IndexBindingContext
 */
public interface IndexedEmbeddedBindingContext extends IndexBindingContext {

	/**
	 * @return The list of index object fields between the parent binding context and this context.
	 */
	Collection<IndexObjectFieldReference> parentIndexObjectReferences();

}
