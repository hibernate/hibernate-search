/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.engine.search.sort.spi.DistanceSortBuilder;
import org.hibernate.search.engine.search.sort.spi.FieldSortBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * Information about a field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <F> The indexed field value type.
 */
public interface LuceneSearchFieldContext<F> extends EventContextProvider {

	String absolutePath();

	String nestedDocumentPath();

	List<String> nestedPathHierarchy();

	boolean multiValuedInRoot();

	LuceneSearchFieldTypeContext<F> type();

	// Query elements: predicates, sorts, projections, aggregations, ...

	<T> T queryElement(SearchQueryElementTypeKey<T> key, LuceneSearchContext searchContext);

	// Predicates

	default MatchPredicateBuilder createMatchPredicateBuilder(
			LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createMatchPredicateBuilder( searchContext, this );
	}

	default RangePredicateBuilder createRangePredicateBuilder(
			LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createRangePredicateBuilder( searchContext, this );
	}

	default PhrasePredicateBuilder createPhrasePredicateBuilder(
			LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createPhrasePredicateBuilder( searchContext, this );
	}

	default WildcardPredicateBuilder createWildcardPredicateBuilder(LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createWildcardPredicateBuilder( searchContext, this );
	}

	default LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState() {
		return type().predicateBuilderFactory().createSimpleQueryStringFieldState( this );
	}

	default ExistsPredicateBuilder createExistsPredicateBuilder(LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createExistsPredicateBuilder( searchContext, this );
	}

	default SpatialWithinCirclePredicateBuilder createSpatialWithinCirclePredicateBuilder(
			LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createSpatialWithinCirclePredicateBuilder( searchContext, this );
	}

	default SpatialWithinPolygonPredicateBuilder createSpatialWithinPolygonPredicateBuilder(
			LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createSpatialWithinPolygonPredicateBuilder( searchContext, this );
	}

	default SpatialWithinBoundingBoxPredicateBuilder createSpatialWithinBoundingBoxPredicateBuilder(
			LuceneSearchContext searchContext) {
		return type().predicateBuilderFactory().createSpatialWithinBoundingBoxPredicateBuilder( searchContext, this );
	}

	// Sorts

	default FieldSortBuilder createFieldSortBuilder(LuceneSearchContext searchContext) {
		return type().sortBuilderFactory().createFieldSortBuilder( searchContext, this );
	}

	default DistanceSortBuilder createDistanceSortBuilder(LuceneSearchContext searchContext,
			GeoPoint center) {
		return type().sortBuilderFactory().createDistanceSortBuilder( searchContext, this, center );
	}

}
