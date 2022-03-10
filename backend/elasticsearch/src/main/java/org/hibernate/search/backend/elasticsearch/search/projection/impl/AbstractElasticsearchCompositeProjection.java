/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Arrays;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;

import com.google.gson.JsonObject;

abstract class AbstractElasticsearchCompositeProjection<P>
		extends AbstractElasticsearchProjection<Object[], P> {

	private final ElasticsearchSearchProjection<?, ?>[] children;

	AbstractElasticsearchCompositeProjection(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchProjection<?, ?> ... children) {
		super( scope );
		this.children = children;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "children=" + Arrays.toString( children )
				+ "]";
	}

	@Override
	public final void request(JsonObject requestBody,
			ProjectionRequestContext context) {
		for ( ElasticsearchSearchProjection<?, ?> child : children ) {
			child.request( requestBody, context );
		}
	}

	@Override
	public final Object[] extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			ProjectionExtractContext context) {
		Object[] extractedData = new Object[children.length];

		for ( int i = 0; i < extractedData.length; i++ ) {
			ElasticsearchSearchProjection<?, ?> child = children[i];
			extractedData[i] = child.extract(
					projectionHitMapper, hit, context
			);
		}

		return extractedData;
	}

	@Override
	public final P transform(LoadingResult<?, ?> loadingResult, Object[] extractedData,
			ProjectionTransformContext context) {
		// Transform in-place
		for ( int i = 0; i < extractedData.length; i++ ) {
			ElasticsearchSearchProjection<?, ?> child = children[i];
			Object extractedElement = extractedData[i];
			extractedData[i] = ElasticsearchSearchProjection.transformUnsafe(
					child, loadingResult, extractedElement, context
			);
		}

		return doTransform( extractedData );
	}

	/**
	 * @param childResults An object array guaranteed to contain
	 * the result of calling {@link ElasticsearchSearchProjection#extract(ProjectionHitMapper, JsonObject, ProjectionExtractContext)},
	 * then {@link ElasticsearchSearchProjection#transform(LoadingResult, Object, ProjectionTransformContext)},
	 * for each child projection.
	 * Each result has the same index as the child projection it originated from.
	 * @return The combination of the child results to return from {@link #transform(LoadingResult, Object[], ProjectionTransformContext)}.
	 */
	abstract P doTransform(Object[] childResults);

	static class Builder<P> implements CompositeProjectionBuilder<P> {

		private final AbstractElasticsearchCompositeProjection<P> projection;

		Builder(AbstractElasticsearchCompositeProjection<P> projection) {
			this.projection = projection;
		}

		@Override
		public SearchProjection<P> build() {
			return projection;
		}
	}
}
