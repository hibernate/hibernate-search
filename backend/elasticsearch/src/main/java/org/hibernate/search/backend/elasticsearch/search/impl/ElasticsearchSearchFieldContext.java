/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateBuilder;
import org.hibernate.search.backend.elasticsearch.search.sort.impl.ElasticsearchSearchSortBuilder;
import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.EventContext;

/**
 * Information about a field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <F> The indexed field value type.
 */
public interface ElasticsearchSearchFieldContext<F> {

	String absolutePath();

	String[] absolutePathComponents();

	List<String> nestedPathHierarchy();

	boolean multiValuedInRoot();

	ElasticsearchSearchFieldTypeContext<F> type();

	EventContext eventContext();

	// Predicates

	default MatchPredicateBuilder<ElasticsearchSearchPredicateBuilder> createMatchPredicateBuilder(
			ElasticsearchSearchContext searchContext) {
		return type().predicateBuilderFactory().createMatchPredicateBuilder( searchContext, this );
	}

	default RangePredicateBuilder<ElasticsearchSearchPredicateBuilder> createRangePredicateBuilder(
			ElasticsearchSearchContext searchContext) {
		return type().predicateBuilderFactory().createRangePredicateBuilder( searchContext, this );
	}

	default PhrasePredicateBuilder<ElasticsearchSearchPredicateBuilder> createPhrasePredicateBuilder() {
		return type().predicateBuilderFactory().createPhrasePredicateBuilder( this );
	}

	default WildcardPredicateBuilder<ElasticsearchSearchPredicateBuilder> createWildcardPredicateBuilder() {
		return type().predicateBuilderFactory().createWildcardPredicateBuilder( this );
	}

	default ElasticsearchSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState() {
		return type().predicateBuilderFactory().createSimpleQueryStringFieldState( this );
	}

	default SpatialWithinCirclePredicateBuilder<ElasticsearchSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder() {
		return type().predicateBuilderFactory().createSpatialWithinCirclePredicateBuilder( this );
	}

	default SpatialWithinPolygonPredicateBuilder<ElasticsearchSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder() {
		return type().predicateBuilderFactory().createSpatialWithinPolygonPredicateBuilder( this );
	}

	default SpatialWithinBoundingBoxPredicateBuilder<ElasticsearchSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder() {
		return type().predicateBuilderFactory().createSpatialWithinBoundingBoxPredicateBuilder( this );
	}

	// Sorts

	default FieldSortBuilder<ElasticsearchSearchSortBuilder> createFieldSortBuilder(
			ElasticsearchSearchContext searchContext) {
		return type().sortBuilderFactory().createFieldSortBuilder( searchContext, this );
	}

	default DistanceSortBuilder<ElasticsearchSearchSortBuilder> createDistanceSortBuilder(
			ElasticsearchSearchContext searchContext, GeoPoint center) {
		return type().sortBuilderFactory().createDistanceSortBuilder( searchContext, this, center );
	}

	// Projections

	default <T> FieldProjectionBuilder<T> createFieldValueProjectionBuilder(ElasticsearchSearchContext searchContext,
			Class<T> expectedType, ValueConvert convert) {
		return type().projectionBuilderFactory().createFieldValueProjectionBuilder( searchContext, this,
				expectedType, convert );
	}

	default DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(ElasticsearchSearchContext searchContext,
			GeoPoint center) {
		return type().projectionBuilderFactory().createDistanceProjectionBuilder( searchContext, this, center );
	}

	// Aggregations

	default <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(ElasticsearchSearchContext searchContext,
			Class<K> expectedType, ValueConvert convert) {
		return type().aggregationBuilderFactory().createTermsAggregationBuilder( searchContext, this,
				expectedType, convert );
	}

	default <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(ElasticsearchSearchContext searchContext,
			Class<K> expectedType, ValueConvert convert) {
		return type().aggregationBuilderFactory().createRangeAggregationBuilder( searchContext, this,
				expectedType, convert );
	}

}
