/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.loading.impl;

import java.lang.invoke.MethodHandles;

import jakarta.persistence.EntityGraph;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.search.mapper.orm.loading.spi.EntityGraphHint;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingMappingContext;
import org.hibernate.search.mapper.orm.loading.spi.HibernateOrmLoadingSessionContext;
import org.hibernate.search.mapper.orm.loading.spi.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmSelectionLoadingContext implements PojoSelectionLoadingContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmLoadingSessionContext sessionContext;
	private final MutableEntityLoadingOptions loadingOptions;
	private final EntityLoadingCacheLookupStrategy cacheLookupStrategy;

	private HibernateOrmSelectionLoadingContext(Builder builder) {
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

	public HibernateOrmLoadingSessionContext sessionContext() {
		return sessionContext;
	}

	public SessionImplementor sessionImplementor() {
		return sessionContext.session();
	}

	public MutableEntityLoadingOptions loadingOptions() {
		return loadingOptions;
	}

	public EntityLoadingCacheLookupStrategy cacheLookupStrategy() {
		return cacheLookupStrategy;
	}

	public static final class Builder
			implements PojoSelectionLoadingContextBuilder<SearchLoadingOptionsStep>, SearchLoadingOptionsStep {
		private final HibernateOrmLoadingSessionContext sessionContext;
		private final MutableEntityLoadingOptions loadingOptions;
		private EntityLoadingCacheLookupStrategy cacheLookupStrategy;

		public Builder(HibernateOrmLoadingMappingContext mappingContext,
				HibernateOrmLoadingSessionContext sessionContext) {
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
}
