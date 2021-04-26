/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.loader;

import org.hibernate.search.mapper.pojo.loading.EntityLoader;
import org.hibernate.search.mapper.pojo.loading.EntityIdentifierScroll;

/**
 * A start loader for entity loading entities during mass indexing.
 *
 * @param <E> The resulting entity type (output)
 * @param <O> The type of options passed to the mass indexer.
 */
public interface MassIndexingEntityLoadingStrategy<E, O> {

	/**
	 * Streams the identifiers of entities to reindex.
	 *
	 * @param options Options passed to the mass indexer.
	 * @param context A mass indexing context for objects to load.
	 * @param loadingTypeGroup The grouping types of loaded objects.
	 * @return A {@link EntityIdentifierScroll}.
	 * @throws java.lang.InterruptedException except where loading interrupted
	 */
	EntityIdentifierScroll createIdentifierScroll(O options, MassIndexingThreadContext context,
			MassIndexingEntityLoadingTypeGroup<? extends E> loadingTypeGroup) throws InterruptedException;

	/**
	 * Loads the entities corresponding to the given identifiers.
	 *
	 * @param options Options passed to the mass indexer.
	 * @param context A mass indexing context for objects to load.
	 * @param loadingTypeGroup The grouping types of loaded objects.
	 * @return A {@link EntityLoader}.
	 * @throws java.lang.InterruptedException except where loading interrupted
	 */
	EntityLoader<E> createLoader(O options, MassIndexingThreadContext context,
			MassIndexingEntityLoadingTypeGroup<? extends E> loadingTypeGroup) throws InterruptedException;

}
