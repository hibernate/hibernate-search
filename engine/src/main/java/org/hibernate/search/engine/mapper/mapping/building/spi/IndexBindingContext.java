/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Optional;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.common.tree.TreeFilterDefinition;
import org.hibernate.search.engine.common.tree.spi.TreeContributionListener;
import org.hibernate.search.engine.mapper.model.spi.MappingElement;

/**
 * The binding context associated to a specific node in the entity tree.
 * <p>
 * The context includes in particular {@link #schemaElement() the corresponding node in the index schema tree}.
 * <p>
 * Provides entry points to add fields to the index schema and to generate new contexts for indexed-embeddeds.
 */
public interface IndexBindingContext {

	/**
	 * @return The type factory of the bound index, allowing to create field types.
	 */
	default IndexFieldTypeFactory createTypeFactory() {
		return createTypeFactory( new IndexFieldTypeDefaultsProvider() );
	}

	/**
	 * Use this method to provide some defaults to the current request.
	 * {@link IndexFieldTypeDefaultsProvider} instance will be overridden by a subsequent request.
	 *
	 * @param defaultsProvider The defaults to apply
	 * @return The type factory of the bound index, allowing to create field types.
	 */
	IndexFieldTypeFactory createTypeFactory(IndexFieldTypeDefaultsProvider defaultsProvider);

	/**
	 * @return The element in the index schema that this context points to.
	 */
	IndexSchemaElement schemaElement();

	/**
	 * @param listener A listener to notify when operations are executed on the returned schema element.
	 * @return The element in the index schema that this context points to,
	 * with a wrapper that ensures the given listener will be called when operations are executed on the schema element.
	 */
	IndexSchemaElement schemaElement(TreeContributionListener listener);

	/**
	 * @param mappingElement A unique representation of this indexed-embedded; if the same indexed-embedded is applied in multiple places,
	 * this method must be called with mapping elements that are equal according to {@link MappingElement#equals(Object)}/{@link MappingElement#hashCode()}.
	 * @param relativePrefix The prefix to apply to all index fields created in the context of the indexed-embedded.
	 * @param structure The structure of all object fields created as part of the {@code relativePrefix}.
	 * @param filter The filter definition (included paths, ...).
	 * @param multiValued Whether the property with an indexed-embedded is to be considered as multi-valued
	 * (i.e. multiple indexed-embedded objects may be processed for a single "embedding" object).
	 * @return A new indexed-embedded binding context, or {@code Optional.empty()}.
	 */
	Optional<IndexedEmbeddedBindingContext> addIndexedEmbeddedIfIncluded(MappingElement mappingElement,
			String relativePrefix,
			ObjectStructure structure,
			TreeFilterDefinition filter,
			boolean multiValued);
}
