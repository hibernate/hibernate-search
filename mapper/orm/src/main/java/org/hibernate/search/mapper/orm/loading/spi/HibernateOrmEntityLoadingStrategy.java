/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;

/**
 * @param <E> The type of loaded entities.
 * @param <I> The type of entity identifiers.
 */
public interface HibernateOrmEntityLoadingStrategy<E, I> {

	/**
	 * @param obj Another strategy
	 * @return {@code true} if the other strategy returns the same type of loaders,
	 * able to target the exact same entity types.
	 * {@code false} otherwise or when unsure.
	 */
	@Override
	boolean equals(Object obj);

	/*
	 * Hashcode must be overridden to be consistent with equals.
	 */
	@Override
	int hashCode();

	<E2> PojoSelectionEntityLoader<E2> createLoader(Set<LoadingTypeContext<? extends E2>> targetEntityTypeContexts,
			LoadingSessionContext sessionContext, EntityLoadingCacheLookupStrategy cacheLookupStrategy,
			MutableEntityLoadingOptions loadingOptions);

	HibernateOrmQueryLoader<E, I> createQueryLoader(List<LoadingTypeContext<? extends E>> typeContexts,
			List<ConditionalExpression> conditionalExpressions);

	HibernateOrmQueryLoader<E, I> createQueryLoader(List<LoadingTypeContext<? extends E>> typeContexts,
			List<ConditionalExpression> conditionalExpressions, String order);

}
