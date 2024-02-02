/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.spi;

import java.util.List;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;

/**
 * @param <E> The type of loaded entities.
 * @param <I> The type of entity identifiers.
 */
public interface HibernateOrmEntityLoadingStrategy<E, I>
		extends PojoSelectionLoadingStrategy<E>, PojoMassLoadingStrategy<E, I> {

	HibernateOrmQueryLoader<E, I> createQueryLoader(SessionFactoryImplementor sessionFactory,
			Set<? extends PojoLoadingTypeContext<? extends E>> typeContexts,
			List<ConditionalExpression> conditionalExpressions);

	HibernateOrmQueryLoader<E, I> createQueryLoader(SessionFactoryImplementor sessionFactory,
			Set<? extends PojoLoadingTypeContext<? extends E>> typeContexts,
			List<ConditionalExpression> conditionalExpressions, String order);

}
