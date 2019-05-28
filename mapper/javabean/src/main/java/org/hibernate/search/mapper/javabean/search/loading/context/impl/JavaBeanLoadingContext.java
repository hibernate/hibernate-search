/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.loading.context.impl;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.mapper.javabean.search.loading.impl.JavaBeanProjectionHitMapper;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;

public final class JavaBeanLoadingContext implements LoadingContext<PojoReference, Void> {

	private final ProjectionHitMapper<PojoReference, Void> projectionHitMapper;

	private JavaBeanLoadingContext(ProjectionHitMapper<PojoReference, Void> projectionHitMapper) {
		this.projectionHitMapper = projectionHitMapper;
	}

	@Override
	public ProjectionHitMapper<PojoReference, Void> getProjectionHitMapper() {
		return projectionHitMapper;
	}

	public static final class Builder implements LoadingContextBuilder<PojoReference, Void> {
		private final PojoScopeDelegate<?, ?> scopeDelegate;

		public Builder(PojoScopeDelegate<?, ?> scopeDelegate) {
			this.scopeDelegate = scopeDelegate;
		}

		@Override
		public LoadingContext<PojoReference, Void> build() {
			ProjectionHitMapper<PojoReference, Void> projectionHitMapper =
					new JavaBeanProjectionHitMapper( scopeDelegate );
			return new JavaBeanLoadingContext( projectionHitMapper );
		}
	}
}
