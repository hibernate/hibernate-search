/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Set;

import org.hibernate.search.mapper.orm.massindexing.impl.MassIndexingTypeLoadingStrategy;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;

/**
 * @param <E> The entity type.
 * @param <I> The identifier type.
 */
public interface EntityLoadingStrategy<E, I> extends MassIndexingTypeLoadingStrategy<E, I> {

	/**
	 * @param obj Another strategy
	 * @return {@code true} if the other strategy returns the same type of loaders,
	 * able to target the exact same entity types.
	 * {@code false} otherwise or when unsure.
	 */
	boolean equals(Object obj);

	/*
	 * Hashcode must be overridden to be consistent with equals.
	 */
	int hashCode();

	<E2> PojoLoader<E2> createLoader(Set<LoadingIndexedTypeContext<? extends E2>> targetEntityTypeContexts,
			LoadingSessionContext sessionContext, EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			MutableEntityLoadingOptions loadingOptions);

}
