/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.common.function.TriFunction;

import com.google.gson.JsonObject;

public class ElasticsearchSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final ElasticsearchSearchIndexScope scope;
	private final DocumentReferenceExtractionHelper documentReferenceExtractionHelper;
	private final ProjectionExtractionHelper<String> idProjectionExtractionHelper;

	public ElasticsearchSearchProjectionBuilderFactory(SearchProjectionBackendContext searchProjectionBackendContext,
			ElasticsearchSearchIndexScope scope) {
		this.scope = scope;
		this.documentReferenceExtractionHelper = searchProjectionBackendContext.createDocumentReferenceExtractionHelper( scope );
		this.idProjectionExtractionHelper = searchProjectionBackendContext.idProjectionExtractionHelper();
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new ElasticsearchDocumentReferenceProjection.Builder( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <T> FieldProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		return scope.field( absoluteFieldPath ).queryElement( ProjectionTypeKeys.FIELD, scope )
				.type( expectedType, convert );
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return new ElasticsearchEntityProjection.Builder<>( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return new ElasticsearchEntityReferenceProjection.Builder<>( scope, documentReferenceExtractionHelper );
	}

	@Override
	public <I> IdProjectionBuilder<I> id(Class<I> identifierType) {
		return new ElasticsearchIdProjection.Builder<>( scope, idProjectionExtractionHelper, identifierType );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new ElasticsearchScoreProjection.Builder( scope );
	}

	@Override
	public DistanceToFieldProjectionBuilder distance(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( ProjectionTypeKeys.DISTANCE, scope );
	}

	@Override
	public <P> CompositeProjectionBuilder<P> composite(Function<List<?>, P> transformer,
			SearchProjection<?>... projections) {
		List<ElasticsearchSearchProjection<?, ?>> typedProjections = new ArrayList<>( projections.length );
		for ( SearchProjection<?> projection : projections ) {
			typedProjections.add( toImplementation( projection ) );
		}

		return new ElasticsearchCompositeListProjection.Builder<>(
				new ElasticsearchCompositeListProjection<>( scope, transformer, typedProjections )
		);
	}

	@Override
	public <P1, P> CompositeProjectionBuilder<P> composite(Function<P1, P> transformer,
			SearchProjection<P1> projection) {
		return new ElasticsearchCompositeFunctionProjection.Builder<>(
				new ElasticsearchCompositeFunctionProjection<>( scope, transformer,
						toImplementation( projection ) )
		);
	}

	@Override
	public <P1, P2, P> CompositeProjectionBuilder<P> composite(BiFunction<P1, P2, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new ElasticsearchCompositeBiFunctionProjection.Builder<>(
				new ElasticsearchCompositeBiFunctionProjection<>( scope, transformer,
						toImplementation( projection1 ), toImplementation( projection2 ) )
		);
	}

	@Override
	public <P1, P2, P3, P> CompositeProjectionBuilder<P> composite(TriFunction<P1, P2, P3, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new ElasticsearchCompositeTriFunctionProjection.Builder<>(
				new ElasticsearchCompositeTriFunctionProjection<>( scope, transformer,
						toImplementation( projection1 ), toImplementation( projection2 ),
						toImplementation( projection3 ) )
		);
	}

	public SearchProjectionBuilder<JsonObject> source() {
		return new ElasticsearchSourceProjection.Builder( scope );
	}

	public SearchProjectionBuilder<JsonObject> explanation() {
		return new ElasticsearchExplanationProjection.Builder( scope );
	}

	public SearchProjectionBuilder<JsonObject> jsonHit() {
		return new ElasticsearchJsonHitProjection.Builder( scope );
	}

	private <T> ElasticsearchSearchProjection<?, T> toImplementation(SearchProjection<T> projection) {
		return ElasticsearchSearchProjection.from( scope, projection );
	}
}
