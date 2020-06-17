/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.predicate.impl;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

/**
 * A field-scoped factory for search predicate builders.
 * <p>
 * Implementations are created and stored for each field at bootstrap,
 * allowing fine-grained control over the type of predicate created for each field.
 * <p>
 * For example, a match predicate on an {@link Integer} field
 * will not have its {@link MatchPredicateBuilder#value(Object, ValueConvert)} method
 * accept the same arguments as a match predicate on a {@link java.time.LocalDate} field;
 * having a separate {@link ElasticsearchFieldPredicateBuilderFactory} for those two fields
 * allows to implement the different behavior.
 * <p>
 * Similarly, and perhaps more importantly,
 * having a per-field factory allows us to throw detailed exceptions
 * when users try to create a predicate that just cannot work on a particular field
 * (either because it has the wrong type, or it's not configured in a way that allows it).
 */
public interface ElasticsearchFieldPredicateBuilderFactory<F> {

	boolean isSearchable();

	boolean isCompatibleWith(ElasticsearchFieldPredicateBuilderFactory<?> other);

	MatchPredicateBuilder createMatchPredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field);

	RangePredicateBuilder createRangePredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field);

	PhrasePredicateBuilder createPhrasePredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field);

	WildcardPredicateBuilder createWildcardPredicateBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field);

	ElasticsearchSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldState(
			ElasticsearchSearchFieldContext<F> field);

	SpatialWithinCirclePredicateBuilder createSpatialWithinCirclePredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field);

	SpatialWithinPolygonPredicateBuilder createSpatialWithinPolygonPredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field);

	SpatialWithinBoundingBoxPredicateBuilder createSpatialWithinBoundingBoxPredicateBuilder(
			ElasticsearchSearchContext searchContext, ElasticsearchSearchFieldContext<F> field);
}
