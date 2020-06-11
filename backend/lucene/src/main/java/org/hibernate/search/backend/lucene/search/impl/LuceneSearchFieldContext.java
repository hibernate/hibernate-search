/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.backend.lucene.search.sort.impl.LuceneSearchSortBuilder;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
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
public interface LuceneSearchFieldContext<F> {

	String absolutePath();

	String nestedDocumentPath();

	List<String> nestedPathHierarchy();

	boolean multiValuedInRoot();

	LuceneSearchFieldTypeContext<F> type();

	EventContext eventContext();

	// Predicates

	default MatchPredicateBuilder<LuceneSearchPredicateBuilder> createMatchPredicateBuilder(
			LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createMatchPredicateBuilder( searchContext, this );
	}

	default RangePredicateBuilder<LuceneSearchPredicateBuilder> createRangePredicateBuilder(
			LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createRangePredicateBuilder( searchContext, this );
	}

	default PhrasePredicateBuilder<LuceneSearchPredicateBuilder> createPhrasePredicateBuilder(
			LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createPhrasePredicateBuilder( searchContext, this );
	}

	default WildcardPredicateBuilder<LuceneSearchPredicateBuilder> createWildcardPredicateBuilder() {
		return type().predicateBuilderFactory().createWildcardPredicateBuilder( this );
	}

	default LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState() {
		return type().predicateBuilderFactory().createSimpleQueryStringFieldState( this );
	}

	default ExistsPredicateBuilder<LuceneSearchPredicateBuilder> createExistsPredicateBuilder() {
		return type().predicateBuilderFactory().createExistsPredicateBuilder( this );
	}

	default SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder() {
		return type().predicateBuilderFactory().createSpatialWithinCirclePredicateBuilder( this );
	}

	default SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder() {
		return type().predicateBuilderFactory().createSpatialWithinPolygonPredicateBuilder( this );
	}

	default SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder() {
		return type().predicateBuilderFactory().createSpatialWithinBoundingBoxPredicateBuilder( this );
	}

	// Sorts

	default FieldSortBuilder<LuceneSearchSortBuilder> createFieldSortBuilder(LuceneSearchContext searchContext) {
		return type().sortBuilderFactory().createFieldSortBuilder( searchContext, this );
	}

	default DistanceSortBuilder<LuceneSearchSortBuilder> createDistanceSortBuilder(GeoPoint center) {
		return type().sortBuilderFactory().createDistanceSortBuilder( this, center );
	}

	// Projections

	default <T> FieldProjectionBuilder<T> createFieldValueProjectionBuilder(LuceneSearchContext searchContext,
			Class<T> expectedType, ValueConvert convert) {
		return type().projectionBuilderFactory().createFieldValueProjectionBuilder( searchContext, this,
				expectedType, convert );
	}

	default DistanceToFieldProjectionBuilder createDistanceProjectionBuilder(LuceneSearchContext searchContext,
			GeoPoint center) {
		return type().projectionBuilderFactory().createDistanceProjectionBuilder( searchContext, this, center );
	}

	// Aggregations

	default <K> TermsAggregationBuilder<K> createTermsAggregationBuilder(LuceneSearchContext searchContext,
			Class<K> expectedType, ValueConvert convert) {
		return type().aggregationBuilderFactory().createTermsAggregationBuilder( searchContext, this,
				expectedType, convert );
	}

	default <K> RangeAggregationBuilder<K> createRangeAggregationBuilder(LuceneSearchContext searchContext,
			Class<K> expectedType, ValueConvert convert) {
		return type().aggregationBuilderFactory().createRangeAggregationBuilder( searchContext, this,
				expectedType, convert );
	}

}
