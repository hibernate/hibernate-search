/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.backend.types.converter.spi.DocumentIdentifierValueConverter;

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

	/**
	 * Order the backend to use the given converter to convert IDs passed to the predicate DSL.
	 *
	 * @param idConverter The ID converter to use in the predicate DSL.
	 */
	void idDslConverter(DocumentIdentifierValueConverter<?> idConverter);

}
