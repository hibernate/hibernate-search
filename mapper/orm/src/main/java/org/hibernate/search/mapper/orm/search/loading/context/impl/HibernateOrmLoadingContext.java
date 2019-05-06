/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.context.impl;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.loading.spi.DefaultProjectionHitMapper;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.mapper.orm.search.loading.impl.MutableObjectLoadingOptions;
import org.hibernate.search.mapper.orm.search.loading.impl.ObjectLoaderBuilder;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;

public final class HibernateOrmLoadingContext<O> implements LoadingContext<PojoReference, O> {
	private final ProjectionHitMapper<PojoReference, O> projectionHitMapper;

	private HibernateOrmLoadingContext(ProjectionHitMapper<PojoReference, O> projectionHitMapper) {
		this.projectionHitMapper = projectionHitMapper;
	}

	@Override
	public ProjectionHitMapper<PojoReference, O> getProjectionHitMapper() {
		return projectionHitMapper;
	}

	public static final class Builder<O> implements LoadingContextBuilder<PojoReference, O> {
		private final PojoSearchScopeDelegate<?, ?> scopeDelegate;
		private final ObjectLoaderBuilder<O> objectLoaderBuilder;
		private final MutableObjectLoadingOptions loadingOptions;

		public Builder(PojoSearchScopeDelegate<?, ?> scopeDelegate,
				ObjectLoaderBuilder<O> objectLoaderBuilder,
				MutableObjectLoadingOptions loadingOptions) {
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
			return new HibernateOrmLoadingContext<>( projectionHitMapper );
		}
	}
}
