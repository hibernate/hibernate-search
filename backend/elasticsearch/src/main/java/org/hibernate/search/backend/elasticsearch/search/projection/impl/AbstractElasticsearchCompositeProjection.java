/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Arrays;
import java.util.Set;

import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonObject;

abstract class AbstractElasticsearchCompositeProjection<P>
		implements ElasticsearchSearchProjection<Object[], P> {

	private final Set<String> indexNames;

	private final ElasticsearchSearchProjection<?, ?>[] children;

	AbstractElasticsearchCompositeProjection(Set<String> indexNames,
			ElasticsearchSearchProjection<?, ?> ... children) {
		this.indexNames = indexNames;
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
			SearchProjectionRequestContext context) {
		for ( ElasticsearchSearchProjection<?, ?> child : children ) {
			child.request( requestBody, context );
		}
	}

	@Override
	public final Object[] extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
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
	public final P transform(LoadingResult<?> loadingResult, Object[] extractedData,
			SearchProjectionTransformContext context) {
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

	@Override
	public final Set<String> getIndexNames() {
		return indexNames;
	}

	/**
	 * @param childResults An object array guaranteed to contain
	 * the result of calling {@link ElasticsearchSearchProjection#extract(ProjectionHitMapper, JsonObject, SearchProjectionExtractContext)},
	 * then {@link ElasticsearchSearchProjection#transform(LoadingResult, Object, SearchProjectionTransformContext)},
	 * for each child projection.
	 * Each result has the same index as the child projection it originated from.
	 * @return The combination of the child results to return from {@link #transform(LoadingResult, Object[], SearchProjectionTransformContext)}.
	 */
	abstract P doTransform(Object[] childResults);

}
