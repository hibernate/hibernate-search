/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;

/**
 * The binding context associated to the root node in the entity tree.
 *
 * @see IndexBindingContext
 */
public interface IndexedEntityBindingContext extends IndexBindingContext {

	/**
	 * Inform the backend that documents for the mapped index will always be provided along
	 * with an explicit routing key,
	 * to be used to route the document to a specific shard.
	 */
	void explicitRouting();

	<I> void idDslConverter(Class<I> valueType, ToDocumentValueConverter<I, String> converter);

	<I> void idProjectionConverter(Class<I> valueType, FromDocumentValueConverter<String, I> converter);

}
