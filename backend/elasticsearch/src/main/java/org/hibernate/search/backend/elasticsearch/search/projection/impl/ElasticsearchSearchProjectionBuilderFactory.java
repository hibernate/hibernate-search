/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.DocumentReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.EntityReferenceProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.function.TriFunction;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonObject;

public class ElasticsearchSearchProjectionBuilderFactory implements SearchProjectionBuilderFactory {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchSearchIndexesContext indexes;
	private final DocumentReferenceExtractionHelper documentReferenceExtractionHelper;

	public ElasticsearchSearchProjectionBuilderFactory(SearchProjectionBackendContext searchProjectionBackendContext,
			ElasticsearchSearchContext searchContext) {
		this.searchContext = searchContext;
		this.indexes = searchContext.indexes();
		this.documentReferenceExtractionHelper =
				searchProjectionBackendContext.createDocumentReferenceExtractionHelper( searchContext );
	}

	@Override
	public DocumentReferenceProjectionBuilder documentReference() {
		return new ElasticsearchDocumentReferenceProjectionBuilder(
				indexes.hibernateSearchIndexNames(),
				documentReferenceExtractionHelper
		);
	}

	@Override
	public <T> FieldProjectionBuilder<T> field(String absoluteFieldPath, Class<T> expectedType, ValueConvert convert) {
		ElasticsearchSearchFieldContext<?> field = indexes.field( absoluteFieldPath );
		// Check the compatibility of nested structure in the case of multi-index search.
		field.nestedPathHierarchy();
		return indexes.field( absoluteFieldPath ).createFieldValueProjectionBuilder( searchContext,
				expectedType, convert );
	}

	@Override
	public <E> EntityProjectionBuilder<E> entity() {
		return new ElasticsearchEntityProjectionBuilder<>(
				indexes.hibernateSearchIndexNames(),
				documentReferenceExtractionHelper
		);
	}

	@Override
	public <R> EntityReferenceProjectionBuilder<R> entityReference() {
		return new ElasticsearchEntityReferenceProjectionBuilder<>(
				indexes.hibernateSearchIndexNames(),
				documentReferenceExtractionHelper
		);
	}

	@Override
	public ScoreProjectionBuilder score() {
		return new ElasticsearchScoreProjectionBuilder( indexes.hibernateSearchIndexNames() );
	}

	@Override
	public DistanceToFieldProjectionBuilder distance(String absoluteFieldPath, GeoPoint center) {
		ElasticsearchSearchFieldContext<?> field = indexes.field( absoluteFieldPath );
		// Check the compatibility of nested structure in the case of multi-index search.
		field.nestedPathHierarchy();
		return field.createDistanceProjectionBuilder( searchContext, center );
	}

	@Override
	public <P> CompositeProjectionBuilder<P> composite(Function<List<?>, P> transformer,
			SearchProjection<?>... projections) {
		List<ElasticsearchSearchProjection<?, ?>> typedProjections = new ArrayList<>( projections.length );
		for ( SearchProjection<?> projection : projections ) {
			typedProjections.add( toImplementation( projection ) );
		}

		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeListProjection<>( indexes.hibernateSearchIndexNames(), transformer, typedProjections )
		);
	}

	@Override
	public <P1, P> CompositeProjectionBuilder<P> composite(Function<P1, P> transformer,
			SearchProjection<P1> projection) {
		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeFunctionProjection<>( indexes.hibernateSearchIndexNames(), transformer, toImplementation( projection ) )
		);
	}

	@Override
	public <P1, P2, P> CompositeProjectionBuilder<P> composite(BiFunction<P1, P2, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2) {
		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeBiFunctionProjection<>( indexes.hibernateSearchIndexNames(), transformer, toImplementation( projection1 ),
						toImplementation( projection2 ) )
		);
	}

	@Override
	public <P1, P2, P3, P> CompositeProjectionBuilder<P> composite(TriFunction<P1, P2, P3, P> transformer,
			SearchProjection<P1> projection1, SearchProjection<P2> projection2, SearchProjection<P3> projection3) {
		return new ElasticsearchCompositeProjectionBuilder<>(
				new ElasticsearchCompositeTriFunctionProjection<>( indexes.hibernateSearchIndexNames(), transformer, toImplementation( projection1 ),
						toImplementation( projection2 ), toImplementation( projection3 ) )
		);
	}

	public SearchProjectionBuilder<JsonObject> source() {
		return new ElasticsearchSourceProjectionBuilder( indexes.hibernateSearchIndexNames() );
	}

	public SearchProjectionBuilder<JsonObject> explanation() {
		return new ElasticsearchExplanationProjectionBuilder( indexes.hibernateSearchIndexNames() );
	}

	public SearchProjectionBuilder<JsonObject> jsonHit() {
		return new ElasticsearchJsonHitProjectionBuilder( indexes.hibernateSearchIndexNames() );
	}

	public <T> ElasticsearchSearchProjection<?, T> toImplementation(SearchProjection<T> projection) {
		if ( !( projection instanceof ElasticsearchSearchProjection ) ) {
			throw log.cannotMixElasticsearchSearchQueryWithOtherProjections( projection );
		}
		ElasticsearchSearchProjection<?, T> casted = (ElasticsearchSearchProjection<?, T>) projection;
		if ( !indexes.hibernateSearchIndexNames().equals( casted.getIndexNames() ) ) {
			throw log.projectionDefinedOnDifferentIndexes( projection, casted.getIndexNames(), indexes.hibernateSearchIndexNames() );
		}
		return casted;
	}
}
