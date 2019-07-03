/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.util.List;

import org.hibernate.Session;

public interface EntityLoaderFactory {

	/**
	 * @param obj Another factory
	 * @return {@code true} if the other factory returns the same type of loaders,
	 * able to target the exact same entity types.
	 * {@code false} otherwise or when unsure.
	 */
	boolean equals(Object obj);

	/*
	 * Hashcode must be overridden to be consistent with equals.
	 */
	int hashCode();

	<E> HibernateOrmComposableEntityLoader<E> create(Class<E> targetEntityType,
			Session session, MutableEntityLoadingOptions loadingOptions);

	<E> HibernateOrmComposableEntityLoader<? extends E> create(List<Class<? extends E>> targetEntityTypes,
			Session session, MutableEntityLoadingOptions loadingOptions);

}
