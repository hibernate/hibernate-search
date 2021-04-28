/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.Set;

import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;

public interface PojoSelectionLoadingContext {

	/**
	 * Check whether this context is still open, throwing an exception if it is not.
	 */
	void checkOpen();

	PojoRuntimeIntrospector runtimeIntrospector();

	/**
	 * @param type The type of entities that will have to be loaded.
	 * @return A "loader key". The loader key allows grouping together types with the same key,
	 * to create a single loader for multiple types.
	 * The main reason to use the same loader key for multiple types is better performance.
	 */
	Object loaderKey(PojoLoadingTypeContext<?> type);

	/**
	 * @param <T> The exposed type of loaded entities.
	 * @param expectedTypes The expected types of loaded objects.
	 * The types are guaranteed to have the same {@link #loaderKey(PojoLoadingTypeContext)}.
	 * @return A loader.
	 * @see PojoSelectionEntityLoader
	 */
	<T> PojoSelectionEntityLoader<? super T> createLoader(Set<PojoLoadingTypeContext<? extends T>> expectedTypes);
}
