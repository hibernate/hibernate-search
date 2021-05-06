/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.spi;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;

/**
 * A strategy for entity loading during mass indexing.
 *
 * @param <E> The type of loaded entities.
 * @param <I> The type of entity identifiers.
 * @param <O> The type of options for mass loading.
 */
public interface PojoMassIndexingLoadingStrategy<E, I, O> {

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
	 * @param context A context, used to retrieve information about targeted types and to create the sink.
	 * @param options Options passed to the mass indexer.
	 * @return An entity identifier loader.
	 */
	PojoMassIdentifierLoader createIdentifierLoader(
			PojoMassIndexingIdentifierLoadingContext<E, I> context, O options);

	/**
	 * @param context A context, used to retrieve information about targeted types and to create the sink.
	 * @param options Options passed to the mass indexer.
	 * @return An entity loader.
	 */
	PojoMassEntityLoader<I> createEntityLoader(PojoMassIndexingEntityLoadingContext<E> context, O options);

}
