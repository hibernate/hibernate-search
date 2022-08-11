/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

/**
 * A projection that ensures its inner projection is executed on the root, not on an object field.
 *
 * @param <P> The type of the element returned by the projection.
 */
public class ElasticsearchRootContextProjection<P>
		extends AbstractElasticsearchProjection<P> {

	private static final JsonObjectAccessor HIT_SOURCE_ACCESSOR =
			JsonAccessor.root().property( "_source" ).asObject();

	private final ElasticsearchSearchProjection<P> inner;

	public ElasticsearchRootContextProjection(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchProjection<P> inner) {
		super( scope );
		this.inner = inner;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inner=" + inner
				+ "]";
	}

	@Override
	public Extractor<?, P> request(JsonObject requestBody, ProjectionRequestContext context) {
		if ( context.absoluteCurrentFieldPath() == null ) {
			// Already being executed in the root context.
			// Avoid unnecessary overhead and skip the wrapping completely:
			return inner.request( requestBody, context );
		}
		ProjectionRequestContext innerContext = context.root();
		return new RootContextExtractor<>( inner.request( requestBody, innerContext ) );
	}

	private static class RootContextExtractor<E, P> implements Extractor<E, P> {
		private final Extractor<E, P> inner;

		private RootContextExtractor(Extractor<E, P> inner) {
			this.inner = inner;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "inner=" + inner
					+ "]";
		}

		@Override
		public E extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit, JsonObject source,
				ProjectionExtractContext context) {
			JsonObject rootSource = HIT_SOURCE_ACCESSOR.get( hit ).orElse( null );
			return inner.extract( projectionHitMapper, hit, rootSource, context );
		}

		@Override
		public P transform(LoadingResult<?, ?> loadingResult, E extractedData, ProjectionTransformContext context) {
			return inner.transform( loadingResult, extractedData, context );
		}
	}
}
