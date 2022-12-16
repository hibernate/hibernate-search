/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassEntityLoader;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassIdentifierLoader;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmMassLoadingOptions;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmQueryLoader;
import org.hibernate.search.mapper.orm.loading.impl.LoadingTypeContext;
import org.hibernate.search.mapper.orm.session.impl.HibernateOrmSessionTypeContextProvider;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassIdentifierSink;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingEntityLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingIdentifierLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public final class HibernateOrmMassIndexingContext
		implements PojoMassIndexingContext, HibernateOrmMassLoadingOptions {
	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final HibernateOrmSessionTypeContextProvider typeContextProvider;
	private final Map<Class<?>, ConditionalExpression> conditionalExpressions = new HashMap<>();
	private CacheMode cacheMode = CacheMode.IGNORE;
	private Integer idLoadingTransactionTimeout;
	private int idFetchSize = 100; //reasonable default as we only load IDs
	private int objectLoadingBatchSize = 10;
	private long objectsLimit = 0; //means no limit at all

	public HibernateOrmMassIndexingContext(HibernateOrmMassIndexingMappingContext mappingContext,
			HibernateOrmSessionTypeContextProvider typeContextContainer) {
		this.mappingContext = mappingContext;
		this.typeContextProvider = typeContextContainer;
	}

	@Override
	public <T> PojoMassIndexingLoadingStrategy<? super T, ?> loadingStrategy(PojoRawTypeIdentifier<T> expectedType) {
		LoadingTypeContext<T> typeContext = typeContextProvider.forExactType( expectedType );
		return new HibernateOrmMassIndexingLoadingStrategy<>( typeContext.loadingStrategy(),
				conditionalExpression( typeContext ), typeContextProvider );
	}

	public void idLoadingTransactionTimeout(int timeoutInSeconds) {
		this.idLoadingTransactionTimeout = timeoutInSeconds;
	}

	@Override
	public Integer idLoadingTransactionTimeout() {
		return idLoadingTransactionTimeout;
	}

	public void cacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	@Override
	public CacheMode cacheMode() {
		return cacheMode;
	}

	public void objectLoadingBatchSize(int batchSize) {
		if ( batchSize < 1 ) {
			throw new IllegalArgumentException( "batchSize must be at least 1" );
		}
		this.objectLoadingBatchSize = batchSize;
	}

	@Override
	public int objectLoadingBatchSize() {
		return objectLoadingBatchSize;
	}

	public void objectsLimit(long maximum) {
		this.objectsLimit = maximum;
	}

	@Override
	public long objectsLimit() {
		return objectsLimit;
	}

	public void idFetchSize(int idFetchSize) {
		// don't check for positive/zero values as it's actually used by some databases
		// as special values which might be useful.
		this.idFetchSize = idFetchSize;
	}

	@Override
	public int idFetchSize() {
		return idFetchSize;
	}

	ConditionalExpression reindexOnly(Class<?> type, String conditionalExpression) {
		ConditionalExpression expression = new ConditionalExpression( conditionalExpression );
		conditionalExpressions.put( type, expression );
		return expression;
	}

	private Optional<ConditionalExpression> conditionalExpression(LoadingTypeContext<?> typeContext) {
		if ( conditionalExpressions.isEmpty() ) {
			return Optional.empty();
		}

		return typeContext.ascendingSuperTypes()
				.stream()
				.map( type -> conditionalExpressions.get( type.javaClass() ) )
				.filter( Objects::nonNull )
				.findFirst();
	}

	private final class HibernateOrmMassIndexingLoadingStrategy<E, I> implements PojoMassIndexingLoadingStrategy<E, I> {

		private final HibernateOrmEntityLoadingStrategy<E, I> delegate;
		private final Optional<ConditionalExpression> conditionalExpression;
		private final HibernateOrmSessionTypeContextProvider typeContextProvider;

		public HibernateOrmMassIndexingLoadingStrategy(HibernateOrmEntityLoadingStrategy<E, I> delegate,
				Optional<ConditionalExpression> conditionalExpression, HibernateOrmSessionTypeContextProvider typeContextProvider) {
			this.delegate = delegate;
			this.conditionalExpression = conditionalExpression;
			this.typeContextProvider = typeContextProvider;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			HibernateOrmMassIndexingLoadingStrategy<?, ?> that = (HibernateOrmMassIndexingLoadingStrategy<?, ?>) o;
			if ( conditionalExpression.isPresent() || that.conditionalExpression.isPresent() ) {
				// Never merge strategies with conditional expressions, it's too complicated to apply a condition to multiple types
				// TODO-4252 Verify if there is a good way to do that
				return false;
			}
			return Objects.equals( delegate, that.delegate );
		}

		@Override
		public int hashCode() {
			return Objects.hash( delegate, conditionalExpression );
		}

		@Override
		public PojoMassIdentifierLoader createIdentifierLoader(PojoMassIndexingIdentifierLoadingContext<E, I> context) {
			SessionFactoryImplementor sessionFactory = mappingContext.sessionFactory();
			List<LoadingTypeContext<? extends E>> typeContexts = context.includedTypes().stream()
					.map( typeContextProvider::forExactType )
					.collect( Collectors.toList() );

			HibernateOrmQueryLoader<E, I> typeQueryLoader = delegate.createQueryLoader(
					typeContexts, conditionalExpression );
			SharedSessionContractImplementor session = (SharedSessionContractImplementor) sessionFactory
					.withStatelessOptions()
					.tenantIdentifier( context.tenantIdentifier() )
					.openStatelessSession();
			try {
				PojoMassIdentifierSink<I> sink = context.createSink();
				return new HibernateOrmMassIdentifierLoader<>( typeQueryLoader,
						HibernateOrmMassIndexingContext.this, sink, session );
			}
			catch (RuntimeException e) {
				new SuppressingCloser( e ).push( SharedSessionContractImplementor::close, session );
				throw e;
			}
		}

		@Override
		public PojoMassEntityLoader<I> createEntityLoader(PojoMassIndexingEntityLoadingContext<E> context) {
			SessionFactoryImplementor sessionFactory = mappingContext.sessionFactory();
			List<LoadingTypeContext<? extends E>> typeContexts = context.includedTypes().stream()
					.map( typeContextProvider::forExactType )
					.collect( Collectors.toList() );

			HibernateOrmQueryLoader<E, ?> typeQueryLoader = delegate.createQueryLoader(
					typeContexts, conditionalExpression );
			SessionImplementor session = (SessionImplementor) sessionFactory
					.withOptions()
					.tenantIdentifier( context.tenantIdentifier() )
					.openSession();
			try {
				session.setHibernateFlushMode( FlushMode.MANUAL );
				session.setCacheMode( cacheMode() );
				session.setDefaultReadOnly( true );

				PojoMassEntitySink<E> sink = context.createSink( mappingContext.sessionContext( session ) );
				return new HibernateOrmMassEntityLoader<>( typeQueryLoader,
						HibernateOrmMassIndexingContext.this, sink, session );
			}
			catch (RuntimeException e) {
				new SuppressingCloser( e ).push( SessionImplementor::close, session );
				throw e;
			}
		}

	}
}
