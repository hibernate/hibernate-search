/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityGraph;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.search.mapper.orm.loading.spi.EntityGraphHint;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmEntityLoadingStrategy;
import org.hibernate.search.mapper.orm.loading.spi.LoadingMappingContext;
import org.hibernate.search.mapper.orm.loading.spi.LoadingSessionContext;
import org.hibernate.search.mapper.orm.loading.spi.LoadingTypeContext;
import org.hibernate.search.mapper.orm.loading.spi.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingStrategy;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmSelectionLoadingContext implements PojoSelectionLoadingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LoadingIndexedTypeContextProvider typeContextProvider;
	private final LoadingSessionContext sessionContext;
	private final MutableEntityLoadingOptions loadingOptions;
	private final EntityLoadingCacheLookupStrategy cacheLookupStrategy;

	private HibernateOrmSelectionLoadingContext(Builder builder) {
		typeContextProvider = builder.typeContextProvider;
		sessionContext = builder.sessionContext;
		loadingOptions = builder.loadingOptions;
		cacheLookupStrategy = builder.cacheLookupStrategy;
	}

	@Override
	public void checkOpen() {
		try {
			sessionContext.session().checkOpen();
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionIsClosed( e );
		}
	}

	@Override
	public PojoRuntimeIntrospector runtimeIntrospector() {
		return sessionContext.runtimeIntrospector();
	}

	@Override
	public <T> PojoSelectionLoadingStrategy<? super T> loadingStrategy(PojoLoadingTypeContext<T> type) {
		return new HibernateOrmSelectionLoadingStrategy<>(
				typeContextProvider.forExactType( type.typeIdentifier() ).loadingStrategy() );
	}

	@Override
	public <T> Optional<PojoSelectionLoadingStrategy<? super T>> loadingStrategyOptional(PojoLoadingTypeContext<T> type) {
		// With the ORM mapper, all (indexed) types can be loaded.
		return Optional.of( loadingStrategy( type ) );
	}

	public SessionImplementor sessionImplementor() {
		return sessionContext.session();
	}

	public MutableEntityLoadingOptions loadingOptions() {
		return loadingOptions;
	}

	public static final class Builder
			implements PojoSelectionLoadingContextBuilder<SearchLoadingOptionsStep>, SearchLoadingOptionsStep {
		private final LoadingIndexedTypeContextProvider typeContextProvider;
		private final LoadingSessionContext sessionContext;
		private final MutableEntityLoadingOptions loadingOptions;
		private EntityLoadingCacheLookupStrategy cacheLookupStrategy;

		public Builder(LoadingMappingContext mappingContext, LoadingIndexedTypeContextProvider typeContextProvider,
				LoadingSessionContext sessionContext) {
			this.typeContextProvider = typeContextProvider;
			this.sessionContext = sessionContext;
			this.loadingOptions = new MutableEntityLoadingOptions( mappingContext );
			this.cacheLookupStrategy = mappingContext.cacheLookupStrategy();
		}

		@Override
		public SearchLoadingOptionsStep toAPI() {
			return this;
		}

		@Override
		public SearchLoadingOptionsStep fetchSize(int fetchSize) {
			loadingOptions.fetchSize( fetchSize );
			return this;
		}

		@Override
		public SearchLoadingOptionsStep cacheLookupStrategy(EntityLoadingCacheLookupStrategy strategy) {
			this.cacheLookupStrategy = strategy;
			return this;
		}

		@Override
		public SearchLoadingOptionsStep graph(EntityGraph<?> graph, GraphSemantic semantic) {
			loadingOptions.entityGraphHint( new EntityGraphHint<>( (RootGraph<?>) graph, semantic ), false );
			return this;
		}

		@Override
		public SearchLoadingOptionsStep graph(String graphName, GraphSemantic semantic) {
			Contracts.assertNotNull( graphName, "graphName" );
			return graph( sessionContext.session().getEntityGraph( graphName ), semantic );
		}

		@Override
		public PojoSelectionLoadingContext build() {
			return new HibernateOrmSelectionLoadingContext( this );
		}
	}

	private class HibernateOrmSelectionLoadingStrategy<E, I> implements PojoSelectionLoadingStrategy<E> {

		private final HibernateOrmEntityLoadingStrategy<E, I> delegate;

		public HibernateOrmSelectionLoadingStrategy(HibernateOrmEntityLoadingStrategy<E, I> delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			HibernateOrmSelectionLoadingStrategy<?, ?> that = (HibernateOrmSelectionLoadingStrategy<?, ?>) o;
			return delegate.equals( that.delegate );
		}

		@Override
		public int hashCode() {
			return delegate.hashCode();
		}

		@Override
		public PojoSelectionEntityLoader<E> createLoader(
				Set<? extends PojoLoadingTypeContext<? extends E>> expectedTypes) {
			Set<LoadingTypeContext<? extends E>> typeContexts = new HashSet<>();
			for ( PojoLoadingTypeContext<? extends E> type : expectedTypes ) {
				LoadingTypeContext<? extends E> typeContext =
						typeContextProvider.forExactType( type.typeIdentifier() );
				typeContexts.add( typeContext );
			}
			return delegate.createLoader( typeContexts, sessionContext, cacheLookupStrategy, loadingOptions );
		}

	}
}
