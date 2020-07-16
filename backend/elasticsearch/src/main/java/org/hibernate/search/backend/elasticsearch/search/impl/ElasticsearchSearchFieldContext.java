/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.List;

import org.hibernate.search.backend.elasticsearch.types.predicate.impl.ElasticsearchSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * Information about a field targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <F> The indexed field value type.
 */
public interface ElasticsearchSearchFieldContext<F> extends EventContextProvider {

	String absolutePath();

	String[] absolutePathComponents();

	List<String> nestedPathHierarchy();

	boolean multiValuedInRoot();

	ElasticsearchSearchFieldTypeContext<F> type();

	// Query elements: predicates, sorts, projections, aggregations, ...

	<T> T queryElement(SearchQueryElementTypeKey<T> key, ElasticsearchSearchContext searchContext);

	// Predicates

	default MatchPredicateBuilder createMatchPredicateBuilder(ElasticsearchSearchContext searchContext) {
		return type().predicateBuilderFactory().createMatchPredicateBuilder( searchContext, this );
	}

	default RangePredicateBuilder createRangePredicateBuilder(ElasticsearchSearchContext searchContext) {
		return type().predicateBuilderFactory().createRangePredicateBuilder( searchContext, this );
	}

	default PhrasePredicateBuilder createPhrasePredicateBuilder(ElasticsearchSearchContext searchContext) {
		return type().predicateBuilderFactory().createPhrasePredicateBuilder( searchContext, this );
	}

	default WildcardPredicateBuilder createWildcardPredicateBuilder(ElasticsearchSearchContext searchContext) {
		return type().predicateBuilderFactory().createWildcardPredicateBuilder( searchContext, this );
	}

	default ElasticsearchSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState() {
		return type().predicateBuilderFactory().createSimpleQueryStringFieldState( this );
	}

	default SpatialWithinCirclePredicateBuilder createSpatialWithinCirclePredicateBuilder(
			ElasticsearchSearchContext searchContext) {
		return type().predicateBuilderFactory().createSpatialWithinCirclePredicateBuilder( searchContext, this );
	}

	default SpatialWithinPolygonPredicateBuilder createSpatialWithinPolygonPredicateBuilder(
			ElasticsearchSearchContext searchContext) {
		return type().predicateBuilderFactory().createSpatialWithinPolygonPredicateBuilder( searchContext, this );
	}

	default SpatialWithinBoundingBoxPredicateBuilder createSpatialWithinBoundingBoxPredicateBuilder(
			ElasticsearchSearchContext searchContext) {
		return type().predicateBuilderFactory().createSpatialWithinBoundingBoxPredicateBuilder( searchContext, this );
	}

}
