/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.IdProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.util.common.function.TriFunction;

import com.google.gson.JsonObject;

public class ElasticsearchSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private final ElasticsearchSearchIndexScope<?> scope;
	private final DocumentReferenceExtractionHelper documentReferenceExtractionHelper;
	private final ProjectionExtractionHelper<String> idProjectionExtractionHelper;

	public ElasticsearchSearchProjectionBuilderFactory(SearchProjectionBackendContext searchProjectionBackendContext,
			ElasticsearchSearchIndexScope<?> scope) {
		this.scope = scope;
		this.documentReferenceExtractionHelper = searchProjectionBackendContext.createDocumentReferenceExtractionHelper( scope );
		this.idProjectionExtractionHelper = searchProjectionBackendContext.idProjectionExtractionHelper();
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new ElasticsearchDocumentReferenceProjection.Builder( scope, documentReferenceExtractionHelper );
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
		SearchIndexIdentifierContext identifier = scope.identifier();
		return new ElasticsearchIdProjection.Builder<>( scope, idProjectionExtractionHelper,
				identifier.projectionConverter().withConvertedType( identifierType, identifier ) );
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new ElasticsearchScoreProjection.Builder( scope );
	}

	@Override
	public <V> CompositeProjectionBuilder<V> composite(Function<List<?>, V> transformer,
			SearchProjection<?>... projections) {
		ElasticsearchSearchProjection<?, ?>[] typedProjections = new ElasticsearchSearchProjection<?, ?>[ projections.length ];
		for ( int i = 0; i < projections.length; i++ ) {
			typedProjections[i] = toImplementation( projections[i] );
		}
		return new ElasticsearchCompositeProjection.Builder<>( scope,
				ProjectionCompositor.fromList( projections.length, transformer ),
				typedProjections );
	}

	@Override
	public <P1, V> CompositeProjectionBuilder<V> composite(Function<P1, V> transformer,
			SearchProjection<P1> projection) {
		return new ElasticsearchCompositeProjection.Builder<>( scope,
				ProjectionCompositor.from( transformer ),
				toImplementation( projection ) );
	}

	@Override
	public <P1, P2, V> CompositeProjectionBuilder<V> composite(BiFunction<P1, P2, V> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new ElasticsearchCompositeProjection.Builder<>( scope,
				ProjectionCompositor.from( transformer ),
				toImplementation( projection1 ), toImplementation( projection2 ) );
	}

	@Override
	public <P1, P2, P3, V> CompositeProjectionBuilder<V> composite(TriFunction<P1, P2, P3, V> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new ElasticsearchCompositeProjection.Builder<>( scope,
				ProjectionCompositor.from( transformer ),
				toImplementation( projection1 ), toImplementation( projection2 ), toImplementation( projection3 ) );
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
