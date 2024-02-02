/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.loading.spi;

import java.util.Set;

/**
 * A strategy for entity loading during search and in indexing plans.
 *
 * @param <E> The type of loaded entities.
 */
public interface PojoSelectionLoadingStrategy<E> {

	/**
	 * @param obj Another strategy
	 * @return {@code true} if the other strategy targets the same entity hierarchy
	 * and can be used as a replacement for this one.
	 * {@code false} otherwise or when unsure.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Hashcode must be overridden to be consistent with equals.
	 */
	@Override
	int hashCode();

	/**
	 * @param expectedTypes The expected types of loaded objects.
	 * The types are guaranteed to be {@link PojoLoadingTypeContext#selectionLoadingStrategy() assigned this strategy}.
	 * @param context
	 *
	 * @return A loader.
	 */
	PojoSelectionEntityLoader<E> createEntityLoader(Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoSelectionLoadingContext context);

}
