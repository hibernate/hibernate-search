/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.search.mapper.orm.common.impl.HibernateOrmUtils;
import org.hibernate.search.mapper.orm.loading.spi.ConditionalExpression;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmQueryLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public abstract class AbstractHibernateOrmLoadingStrategy<E, I>
		implements HibernateOrmEntityLoadingStrategy<E, I> {

	protected final String rootEntityName;
	private final TypeQueryFactory<E, I> queryFactory;

	AbstractHibernateOrmLoadingStrategy(String rootEntityName, TypeQueryFactory<E, I> queryFactory) {
		this.rootEntityName = rootEntityName;
		this.queryFactory = queryFactory;
	}

	@Override
	public final PojoSelectionEntityLoader<E> createEntityLoader(
			Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoSelectionLoadingContext context) {
		var ormContext = (HibernateOrmSelectionLoadingContext) context;
		return createEntityLoader( expectedTypes, ormContext );
	}

	public abstract PojoSelectionEntityLoader<E> createEntityLoader(
			Set<? extends PojoLoadingTypeContext<? extends E>> targetEntityTypeContexts,
			HibernateOrmSelectionLoadingContext loadingContext);

	@Override
	public final boolean groupingAllowed(PojoLoadingTypeContext<? extends E> type, PojoMassLoadingContext context) {
		// Only allow grouping for types that don't have a conditional expression:
		// it's too complicated to apply a condition to multiple types in the same query.
		// TODO HSEARCH-4252 Apply a condition to multiple types in the same query
		return ( (HibernateOrmMassLoadingContext) context ).conditionalExpression( type ).isEmpty();
	}

	@Override
	public final PojoMassIdentifierLoader createIdentifierLoader(
			Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoMassIdentifierLoadingContext<I> context) {
		var ormContext = (HibernateOrmMassLoadingContext) context.parent();
		SessionFactoryImplementor sessionFactory = ormContext.mapping().sessionFactory();

		HibernateOrmQueryLoader<E, I> queryLoader = createQueryLoader( sessionFactory, expectedTypes,
				conditionalExpressions( expectedTypes, ormContext ) );
		SharedSessionContractImplementor session = (SharedSessionContractImplementor) sessionFactory
				.withStatelessOptions()
				.tenantIdentifier( (Object) context.tenantIdentifier() )
				.openStatelessSession();
		try {
			PojoMassIdentifierSink<I> sink = context.createSink();
			return new HibernateOrmMassIdentifierLoader<>( queryLoader, ormContext, sink, session );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( SharedSessionContractImplementor::close, session );
			throw e;
		}
	}

	@Override
	public final PojoMassEntityLoader<I> createEntityLoader(Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			PojoMassEntityLoadingContext<E> context) {
		var ormContext = (HibernateOrmMassLoadingContext) context.parent();
		SessionFactoryImplementor sessionFactory = ormContext.mapping().sessionFactory();

		HibernateOrmQueryLoader<E, ?> queryLoader = createQueryLoader( sessionFactory, expectedTypes,
				conditionalExpressions( expectedTypes, ormContext ) );
		SessionImplementor session = (SessionImplementor) sessionFactory
				.withOptions()
				.tenantIdentifier( (Object) context.tenantIdentifier() )
				.openSession();
		try {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			session.setCacheMode( ormContext.cacheMode() );
			session.setDefaultReadOnly( true );

			PojoMassEntitySink<E> sink = context.createSink( ormContext.mapping().sessionContext( session ) );
			return new HibernateOrmMassEntityLoader<>( queryLoader, ormContext, sink, session );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( SessionImplementor::close, session );
			throw e;
		}
	}

	private List<ConditionalExpression> conditionalExpressions(Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes,
			HibernateOrmMassLoadingContext context) {
		if ( expectedTypes.size() != 1 ) {
			// We know there's no condition, see groupingAllowed()
			// TODO HSEARCH-4252 Apply a condition to multiple types in the same query
			return List.of();
		}
		var condition = context.conditionalExpression( expectedTypes.iterator().next() );
		return condition.isPresent() ? List.of( condition.get() ) : List.of();
	}

	@Override
	public HibernateOrmQueryLoader<E, I> createQueryLoader(SessionFactoryImplementor sessionFactory,
			Set<? extends PojoLoadingTypeContext<? extends E>> typeContexts,
			List<ConditionalExpression> conditionalExpressions) {
		return createQueryLoader( sessionFactory, typeContexts, conditionalExpressions, null );
	}

	@Override
	public HibernateOrmQueryLoader<E, I> createQueryLoader(SessionFactoryImplementor sessionFactory,
			Set<? extends PojoLoadingTypeContext<? extends E>> typeContexts,
			List<ConditionalExpression> conditionalExpressions, String order) {
		var rootEntityMappingType = HibernateOrmUtils.entityMappingType( sessionFactory, rootEntityName );
		Set<Class<? extends E>> includedTypesFilter;
		if ( HibernateOrmUtils.targetsAllConcreteSubTypes( sessionFactory, rootEntityMappingType, typeContexts ) ) {
			// All concrete types are included, no need to filter by type.
			includedTypesFilter = Collections.emptySet();
		}
		else {
			includedTypesFilter = new HashSet<>( typeContexts.size() );
			for ( PojoLoadingTypeContext<? extends E> typeContext : typeContexts ) {
				includedTypesFilter.add( typeContext.typeIdentifier().javaClass() );
			}
		}

		if ( !conditionalExpressions.isEmpty() || order != null ) {
			if ( typeContexts.size() != 1 ) {
				// TODO HSEARCH-4252 Apply a condition to multiple types in the same query
				throw new AssertionFailure( "conditional/order expression is always defined on a single type" );
			}

			EntityMappingType entityMappingType = HibernateOrmUtils.entityMappingType( sessionFactory,
					typeContexts.iterator().next().secondaryEntityName() );
			return new HibernateOrmQueryLoaderImpl<>( queryFactory, entityMappingType,
					includedTypesFilter, conditionalExpressions, order );
		}
		return new HibernateOrmQueryLoaderImpl<>( queryFactory, includedTypesFilter );
	}

}
