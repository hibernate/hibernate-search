/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.context.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import javax.persistence.EntityGraph;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.loading.spi.DefaultProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.EntityLoader;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityGraphHint;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityLoaderBuilder;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmLoadingMappingContext;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmLoadingSessionContext;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmLoadingContext<E> implements LoadingContext<EntityReference, E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SessionImplementor sessionImplementor;
	private final DocumentReferenceConverter<EntityReference> referenceHitMapper;
	private final EntityLoader<EntityReference, ? extends E> entityLoader;
	private final MutableEntityLoadingOptions loadingOptions;

	private HibernateOrmLoadingContext(SessionImplementor sessionImplementor,
			DocumentReferenceConverter<EntityReference> referenceHitMapper,
			EntityLoader<EntityReference, ? extends E> entityLoader,
			MutableEntityLoadingOptions loadingOptions) {
		this.sessionImplementor = sessionImplementor;
		this.referenceHitMapper = referenceHitMapper;
		this.entityLoader = entityLoader;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public ProjectionHitMapper<EntityReference, E> createProjectionHitMapper() {
		try {
			sessionImplementor.checkOpen();
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionIsClosed( e );
		}

		return new DefaultProjectionHitMapper<>( referenceHitMapper, entityLoader );
	}

	public SessionImplementor sessionImplementor() {
		return sessionImplementor;
	}

	public MutableEntityLoadingOptions loadingOptions() {
		return loadingOptions;
	}

	public static final class Builder<E>
			implements LoadingContextBuilder<EntityReference, E, SearchLoadingOptionsStep>, SearchLoadingOptionsStep {
		private final HibernateOrmLoadingSessionContext sessionContext;
		private final EntityLoaderBuilder<E> entityLoaderBuilder;
		private final MutableEntityLoadingOptions loadingOptions;

		public Builder(HibernateOrmLoadingMappingContext mappingContext,
				HibernateOrmLoadingSessionContext sessionContext,
				Set<HibernateOrmScopeIndexedTypeContext<? extends E>> indexedTypeContexts) {
			this.sessionContext = sessionContext;
			this.entityLoaderBuilder = new EntityLoaderBuilder<>( mappingContext, sessionContext, indexedTypeContexts );
			this.loadingOptions = new MutableEntityLoadingOptions( mappingContext );
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
			entityLoaderBuilder.cacheLookupStrategy( strategy );
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
		public LoadingContext<EntityReference, E> build() {
			DocumentReferenceConverter<EntityReference> referenceHitMapper = sessionContext.referenceHitMapper();
			EntityLoader<EntityReference, ? extends E> entityLoader = entityLoaderBuilder.build( loadingOptions );
			return new HibernateOrmLoadingContext<>(
					sessionContext.session(),
					referenceHitMapper, entityLoader,
					loadingOptions
			);
		}
	}
}
