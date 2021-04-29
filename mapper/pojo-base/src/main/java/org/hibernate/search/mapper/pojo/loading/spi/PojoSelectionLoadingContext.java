/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

public interface PojoSelectionLoadingContext {

	/**
	 * Check whether this context is still open, throwing an exception if it is not.
	 */
	void checkOpen();

	PojoRuntimeIntrospector runtimeIntrospector();

	/**
	 * @param <T> The type of entities that will have to be loaded.
	 * @param type The type of entities that will have to be loaded.
	 * @return A loading strategy.
	 * Note that different types with the same strategy will be grouped together and loaded with a single loader.
	 * @throws org.hibernate.search.util.common.SearchException if the given type cannot be loaded and thus has no loading strategy.
	 * @see PojoSelectionLoadingStrategy#createLoader(Set)
	 */
	<T> PojoSelectionLoadingStrategy<? super T> loadingStrategy(PojoLoadingTypeContext<T> type);

	/**
	 * @param <T> The type of entities that will have to be loaded.
	 * @param type The type of entities that will have to be loaded.
	 * @return A loading strategy, or {@link Optional#empty()} if the given type cannot be loaded and thus has no loading strategy.
	 * Note that different types with the same strategy will be grouped together and loaded with a single loader.
	 * @see PojoSelectionLoadingStrategy#createLoader(Set)
	 */
	<T> Optional<PojoSelectionLoadingStrategy<? super T>> loadingStrategyOptional(PojoLoadingTypeContext<T> type);

}
