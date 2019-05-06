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
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.impl.ObjectLoaderBuilder;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public final class HibernateOrmLoadingContext<O> implements LoadingContext<PojoReference, O> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SessionImplementor sessionImplementor;

	private final ProjectionHitMapper<PojoReference, O> projectionHitMapper;

	private HibernateOrmLoadingContext(SessionImplementor sessionImplementor,
			ProjectionHitMapper<PojoReference, O> projectionHitMapper) {
		this.sessionImplementor = sessionImplementor;
		this.projectionHitMapper = projectionHitMapper;
	}

	@Override
	public ProjectionHitMapper<PojoReference, O> getProjectionHitMapper() {
		try {
			sessionImplementor.checkOpen();
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionIsClosed( e );
		}

		return projectionHitMapper;
	}

	public static final class Builder<O> implements LoadingContextBuilder<PojoReference, O> {
		private final SessionImplementor sessionImplementor;
		private final PojoSearchScopeDelegate<?, ?> scopeDelegate;
		private final ObjectLoaderBuilder<O> objectLoaderBuilder;
		private final MutableObjectLoadingOptions loadingOptions;

		public Builder(SessionImplementor sessionImplementor,
				PojoSearchScopeDelegate<?, ?> scopeDelegate,
				ObjectLoaderBuilder<O> objectLoaderBuilder,
				MutableObjectLoadingOptions loadingOptions) {
			this.sessionImplementor = sessionImplementor;
			this.scopeDelegate = scopeDelegate;
			this.objectLoaderBuilder = objectLoaderBuilder;
			this.loadingOptions = loadingOptions;
		}

		@Override
		public LoadingContext<PojoReference, O> build() {
			ProjectionHitMapper<PojoReference, O> projectionHitMapper = new DefaultProjectionHitMapper<>(
					scopeDelegate::toPojoReference,
					objectLoaderBuilder.build( loadingOptions )
			);
			return new HibernateOrmLoadingContext<>( sessionImplementor, projectionHitMapper );
		}
	}
}
