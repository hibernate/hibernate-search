/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.AssertionFailure;

public abstract class AbstractHibernateOrmLoadingStrategy<E, I>
		implements HibernateOrmEntityLoadingStrategy<E, I> {

	private final SessionFactoryImplementor sessionFactory;
	private final EntityPersister rootEntityPersister;
	private final TypeQueryFactory<E, I> queryFactory;

	AbstractHibernateOrmLoadingStrategy(SessionFactoryImplementor sessionFactory,
			EntityPersister rootEntityPersister, TypeQueryFactory<E, I> queryFactory) {
		this.sessionFactory = sessionFactory;
		this.rootEntityPersister = rootEntityPersister;
		this.queryFactory = queryFactory;
	}

	@Override
	public HibernateOrmQueryLoader<E, I> createQueryLoader(
			Collection<PojoRawTypeIdentifier<? extends E>> targetEntityTypes,
			List<LoadingTypeContext<? extends E>> typeContexts, Optional<String> conditionalExpression) {
		Set<Class<? extends E>> includedTypesFilter;
		if ( HibernateOrmUtils.targetsAllConcreteSubTypes( sessionFactory, rootEntityPersister, targetEntityTypes ) ) {
			// All concrete types are included, no need to filter by type.
			includedTypesFilter = Collections.emptySet();
		}
		else {
			includedTypesFilter = new HashSet<>( targetEntityTypes.size() );
			for ( PojoRawTypeIdentifier<? extends E> targetEntityType : targetEntityTypes ) {
				includedTypesFilter.add( targetEntityType.javaClass() );
			}
		}

		if ( conditionalExpression.isPresent() ) {
			if ( typeContexts.size() != 1 ) {
				throw new AssertionFailure( "conditional expression is always defined on a single type" );
			}

			EntityPersister entityPersister = typeContexts.get( 0 ).entityPersister();
			return new HibernateOrmQueryLoader<>(
					queryFactory, entityPersister, includedTypesFilter, conditionalExpression.get() );
		}
		return new HibernateOrmQueryLoader<>( queryFactory, includedTypesFilter );
	}

}
