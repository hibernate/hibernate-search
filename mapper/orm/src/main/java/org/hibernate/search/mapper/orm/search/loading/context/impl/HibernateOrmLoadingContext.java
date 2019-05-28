/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.context.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.loading.spi.DefaultProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.search.loading.impl.EntityLoaderBuilder;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableEntityLoadingOptions;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmLoadingContext<E> implements LoadingContext<PojoReference, E> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SessionImplementor sessionImplementor;

	private final ProjectionHitMapper<PojoReference, E> projectionHitMapper;

	private final MutableEntityLoadingOptions loadingOptions;

	private HibernateOrmLoadingContext(SessionImplementor sessionImplementor,
			ProjectionHitMapper<PojoReference, E> projectionHitMapper,
			MutableEntityLoadingOptions loadingOptions) {
		this.sessionImplementor = sessionImplementor;
		this.projectionHitMapper = projectionHitMapper;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public ProjectionHitMapper<PojoReference, E> getProjectionHitMapper() {
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

	public static final class Builder<E> implements LoadingContextBuilder<PojoReference, E> {
		private final SessionImplementor sessionImplementor;
		private final PojoScopeDelegate<?, ?> scopeDelegate;
		private final EntityLoaderBuilder<E> entityLoaderBuilder;
		private final MutableEntityLoadingOptions loadingOptions;

		public Builder(SessionImplementor sessionImplementor,
				PojoScopeDelegate<?, ?> scopeDelegate,
				EntityLoaderBuilder<E> entityLoaderBuilder,
				MutableEntityLoadingOptions loadingOptions) {
			this.sessionImplementor = sessionImplementor;
			this.scopeDelegate = scopeDelegate;
			this.entityLoaderBuilder = entityLoaderBuilder;
			this.loadingOptions = loadingOptions;
		}

		@Override
		public LoadingContext<PojoReference, E> build() {
			ProjectionHitMapper<PojoReference, E> projectionHitMapper = new DefaultProjectionHitMapper<>(
					scopeDelegate::toPojoReference,
					entityLoaderBuilder.build( loadingOptions )
			);
			return new HibernateOrmLoadingContext<>( sessionImplementor, projectionHitMapper, loadingOptions );
		}
	}
}
