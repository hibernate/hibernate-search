/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;

/**
 * Contextual information about a mass indexing proccess.
 * @param <O> The type of options passed to the mass indexer.
 */
public interface PojoMassIndexingContext<O> {

	/**
	 * @param <T> The exposed type of indexed entities.
	 * @param expectedType The expected types of indexed objects.
	 * @return A loading strategy.
	 */
	<T> PojoMassIndexingLoadingStrategy<? super T, ?, O> loadingStrategy(PojoRawTypeIdentifier<T> expectedType);

}
