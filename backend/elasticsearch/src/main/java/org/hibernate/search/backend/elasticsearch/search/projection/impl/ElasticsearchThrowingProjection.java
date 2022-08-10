/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.function.Supplier;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.SearchException;

import com.google.gson.JsonObject;

public class ElasticsearchThrowingProjection<T> extends AbstractElasticsearchProjection<T>
		implements ElasticsearchSearchProjection.Extractor<Object, T> {
	private final Supplier<SearchException> exceptionSupplier;

	public ElasticsearchThrowingProjection(ElasticsearchSearchIndexScope<?> scope,
			Supplier<SearchException> exceptionSupplier) {
		super( scope );
		this.exceptionSupplier = exceptionSupplier;
	}

	@Override
	public Extractor<?, T> request(JsonObject requestBody, ProjectionRequestContext context) {
		return this;
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit, JsonObject source,
			ProjectionExtractContext context) {
		throw exceptionSupplier.get();
	}

	@Override
	public T transform(LoadingResult<?, ?> loadingResult, Object extractedData, ProjectionTransformContext context) {
		throw exceptionSupplier.get();
	}
}
