/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.document.model.dsl.spi;

import org.hibernate.search.engine.backend.types.converter.FromDocumentValueConverter;
import org.hibernate.search.engine.backend.types.converter.ToDocumentValueConverter;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexFieldTypeDefaultsProvider;

public interface IndexRootBuilder extends IndexCompositeNodeBuilder {

	IndexFieldTypeFactory createTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider);

	/**
	 * Inform the model collector that documents will always be provided along
	 * with an explicit routing key,
	 * to be used to route the document to a specific shard.
	 */
	void explicitRouting();

	/**
	 * Defines how identifier values passed to the search DSL should be converted to document identifiers.
	 * <p>
	 * When not set, users are expected to pass document identifiers directly.
	 *
	 * @param valueType The type of values that can be passed to the DSL.
	 * @param converter A converter from the given value type to the document identifier (a string).
	 * @param <I> The type of identifier values that can be passed to the DSL.
	 */
	<I> void idDslConverter(Class<I> valueType, ToDocumentValueConverter<I, String> converter);

	/**
	 * Define how values returned when projecting on identifiers
	 * should be converted before being returned to the user.
	 * <p>
	 * When not set, users will be returned the document identifier directly.
	 *
	 * @param valueType The type of values that will be returned when projecting on fields of this type.
	 * @param converter A converter from the document identifier (a string) to the given value type.
	 * @param <I> The type of values that will be returned when projecting on identifiers.
	 */
	<I> void idProjectionConverter(Class<I> valueType, FromDocumentValueConverter<String, I> converter);

}
