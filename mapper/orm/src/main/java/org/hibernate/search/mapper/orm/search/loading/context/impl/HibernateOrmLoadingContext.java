/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.context.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.loading.spi.DefaultProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityLoaderBuilder;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmLoadingMappingContext;
import org.hibernate.search.mapper.orm.search.loading.impl.HibernateOrmLoadingSessionContext;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmLoadingContext<E> implements LoadingContext<EntityReference, E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SessionImplementor sessionImplementor;

	private final ProjectionHitMapper<EntityReference, E> projectionHitMapper;

	private final MutableEntityLoadingOptions loadingOptions;

	private HibernateOrmLoadingContext(SessionImplementor sessionImplementor,
			ProjectionHitMapper<EntityReference, E> projectionHitMapper,
			MutableEntityLoadingOptions loadingOptions) {
		this.sessionImplementor = sessionImplementor;
		this.projectionHitMapper = projectionHitMapper;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public ProjectionHitMapper<EntityReference, E> getProjectionHitMapper() {
		try {
			sessionImplementor.checkOpen();
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionIsClosed( e );
		}

		return projectionHitMapper;
	}

	public SessionImplementor getSessionImplementor() {
		return sessionImplementor;
	}

	public MutableEntityLoadingOptions getLoadingOptions() {
		return loadingOptions;
	}

	public static final class Builder<E> implements LoadingContextBuilder<EntityReference, E> {
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

		public void fetchSize(int fetchSize) {
			loadingOptions.setFetchSize( fetchSize );
		}

		public void cacheLookupStrategy(EntityLoadingCacheLookupStrategy strategy) {
			entityLoaderBuilder.cacheLookupStrategy( strategy );
		}

		@Override
		public LoadingContext<EntityReference, E> build() {
			ProjectionHitMapper<EntityReference, E> projectionHitMapper = new DefaultProjectionHitMapper<>(
					sessionContext.getReferenceHitMapper(),
					entityLoaderBuilder.build( loadingOptions )
			);
			return new HibernateOrmLoadingContext<>(
					sessionContext.getSession(),
					projectionHitMapper,
					loadingOptions
			);
		}
	}
}
