/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFactory;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;

/**
 * The binding context associated to a specific node in the entity tree.
 * <p>
 * The context includes in particular {@link #getSchemaElement() the corresponding node in the index schema tree}.
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
	IndexSchemaElement getSchemaElement();

	/**
	 * @param listener A listener to notify when operations are executed on the returned schema element.
	 * @return The element in the index schema that this context points to,
	 * with a wrapper that ensures the given listener will be called when operations are executed on the schema element.
	 */
	IndexSchemaElement getSchemaElement(IndexSchemaContributionListener listener);

	/**
	 * @param parentTypeModel The model representing the type holding the property with an indexed-embedded.
	 * @param multiValued Whether the property with an indexed-embedded is to be considered as multi-valued
	 * (i.e. multiple indexed-embedded objects may be processed for a single "embedding" object).
	 * @param relativePrefix The prefix to apply to all index fields created in the context of the indexed-embedded.
	 * @param storage The storage type to use for all object fields created as part of the {@code relativePrefix}.
	 * @param maxDepth The maximum depth beyond which all created fields will be ignored. {@code null} for no limit.
	 * @param includePaths The exhaustive list of paths of fields that are to be included. {@code null} for no limit.
	 * @return The element in the index schema that this context points to.
	 */
	Optional<IndexedEmbeddedBindingContext> addIndexedEmbeddedIfIncluded(
			MappableTypeModel parentTypeModel, boolean multiValued,
			String relativePrefix, ObjectFieldStorage storage, Integer maxDepth, Set<String> includePaths);
}
