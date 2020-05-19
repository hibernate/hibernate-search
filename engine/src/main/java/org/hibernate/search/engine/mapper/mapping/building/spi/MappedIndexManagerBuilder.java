/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.mapper.mapping.building.spi;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager;

/**
 * A builder for {@link org.hibernate.search.engine.mapper.mapping.spi.MappedIndexManager} instances,
 * which will be the interface between the mapping and the index when indexing and searching.
 * <p>
 * Exposes in particular the {@link IndexedEntityBindingContext binding context},
 * allowing the mapper to declare index fields that will be bound to entity properties.
 */
public interface MappedIndexManagerBuilder {

	String indexName();

	IndexedEntityBindingContext rootBindingContext();

	MappedIndexManager build();

}
