/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.loading;

/**
 * A strategy for selection loading, used in particular during search.
 *
 * @param <E> The type of loaded entities.
 */
public interface SelectionLoadingStrategy<E> {

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
	 * @param includedTypes A representation of all entity types that will have to be loaded.
	 * @param options Loading options configured by the requester (who created the session, requested the search, ...).
	 * @return An entity loader.
	 */
	SelectionEntityLoader<E> createEntityLoader(LoadingTypeGroup<E> includedTypes, SelectionLoadingOptions options);

}
