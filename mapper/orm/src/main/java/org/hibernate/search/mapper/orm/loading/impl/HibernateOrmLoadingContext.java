/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityGraph;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.loading.spi.PojoLoadingTypeContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmLoadingContext implements PojoLoadingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LoadingIndexedTypeContextProvider typeContextProvider;
	private final LoadingSessionContext sessionContext;
	private final MutableEntityLoadingOptions loadingOptions;
	private final EntityLoadingCacheLookupStrategy cacheLookupStrategy;

	private HibernateOrmLoadingContext(Builder builder) {
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
	public Object loaderKey(PojoLoadingTypeContext<?> type) {
		return typeContextProvider.indexedForExactType( type.typeIdentifier() ).loadingStrategy();
	}

	@Override
	public <T> PojoLoader<T> createLoader(Set<PojoLoadingTypeContext<? extends T>> expectedTypes) {
		if ( expectedTypes.size() == 1 ) {
			// Optimization: no need for the checks below if there's only one type.
			LoadingIndexedTypeContext<? extends T> typeContext = typeContextProvider.indexedForExactType( expectedTypes.iterator().next().typeIdentifier() );
			return typeContext.loadingStrategy().createLoader( Collections.singleton( typeContext ), sessionContext,
					cacheLookupStrategy, loadingOptions );
		}

		HibernateOrmEntityLoadingStrategy<?, ?> loadingStrategy = null;
		Set<LoadingIndexedTypeContext<? extends T>> typeContexts = new HashSet<>();
		for ( PojoLoadingTypeContext<? extends T> type : expectedTypes ) {
			LoadingIndexedTypeContext<? extends T> typeContext = typeContextProvider.indexedForExactType( type.typeIdentifier() );
			typeContexts.add( typeContext );
			HibernateOrmEntityLoadingStrategy<?, ?> thisTypeLoadingStrategy = typeContext.loadingStrategy();
			if ( loadingStrategy == null ) {
				loadingStrategy = thisTypeLoadingStrategy;
			}
			else if ( !loadingStrategy.equals( thisTypeLoadingStrategy ) ) {
				throw new AssertionFailure(
						"Some types among the targeted entity types have a different (incompatible) entity loading strategy."
								+ " Offending entity names: "
								+ typeContexts.stream()
										.map( LoadingIndexedTypeContext::entityPersister )
										.map( EntityPersister::getEntityName )
										.collect( Collectors.toList() )
				);
			}
		}
		if ( loadingStrategy == null ) {
			throw new AssertionFailure( "Attempt to create a loader targeting no type at all." );
		}
		return loadingStrategy.createLoader( typeContexts, sessionContext, cacheLookupStrategy, loadingOptions );
	}

	public SessionImplementor sessionImplementor() {
		return sessionContext.session();
	}

	public MutableEntityLoadingOptions loadingOptions() {
		return loadingOptions;
	}

	public static final class Builder
			implements PojoLoadingContextBuilder<SearchLoadingOptionsStep>, SearchLoadingOptionsStep {
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
		public PojoLoadingContext build() {
			return new HibernateOrmLoadingContext( this );
		}
	}
}
