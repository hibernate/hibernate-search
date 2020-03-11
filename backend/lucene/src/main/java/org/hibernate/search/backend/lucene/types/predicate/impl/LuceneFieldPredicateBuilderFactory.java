/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.predicate.impl;

import java.util.List;

import org.hibernate.search.backend.lucene.scope.model.impl.LuceneCompatibilityChecker;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicateBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
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
 * allowing fine-grained control over the type of predicate created for each field.
 * <p>
 * For example, a match predicate on an {@link Integer} field
 * will not have its {@link MatchPredicateBuilder#value(Object, ValueConvert)} method
 * accept the same arguments as a match predicate on a {@link java.time.LocalDate} field;
 * having a separate {@link LuceneFieldPredicateBuilderFactory} for those two fields
 * allows to implement the different behavior.
 * <p>
 * Similarly, and perhaps more importantly,
 * having a per-field factory allows us to throw detailed exceptions
 * when users try to create a predicate that just cannot work on a particular field
 * (either because it has the wrong type, or it's not configured in a way that allows it).
 */
public interface LuceneFieldPredicateBuilderFactory {

	boolean hasCompatibleCodec(LuceneFieldPredicateBuilderFactory other);

	boolean hasCompatibleConverter(LuceneFieldPredicateBuilderFactory other);

	boolean hasCompatibleAnalyzer(LuceneFieldPredicateBuilderFactory other);

	MatchPredicateBuilder<LuceneSearchPredicateBuilder> createMatchPredicateBuilder( LuceneSearchContext searchContext, String absoluteFieldPath, List<String> nestedPathHierarchy,
			LuceneCompatibilityChecker converterChecker, LuceneCompatibilityChecker analyzerChecker);

	RangePredicateBuilder<LuceneSearchPredicateBuilder> createRangePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, LuceneCompatibilityChecker converterChecker);

	PhrasePredicateBuilder<LuceneSearchPredicateBuilder> createPhrasePredicateBuilder(
			LuceneSearchContext searchContext, String absoluteFieldPath, LuceneCompatibilityChecker analyzerChecker);

	WildcardPredicateBuilder<LuceneSearchPredicateBuilder> createWildcardPredicateBuilder(
			String absoluteFieldPath);

	LuceneSimpleQueryStringPredicateBuilderFieldState createSimpleQueryStringFieldContext(String absoluteFieldPath);

	ExistsPredicateBuilder<LuceneSearchPredicateBuilder> createExistsPredicateBuilder(String absoluteFieldPath);

	SpatialWithinCirclePredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinCirclePredicateBuilder(
			String absoluteFieldPath);

	SpatialWithinPolygonPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinPolygonPredicateBuilder(
			String absoluteFieldPath);

	SpatialWithinBoundingBoxPredicateBuilder<LuceneSearchPredicateBuilder> createSpatialWithinBoundingBoxPredicateBuilder(
			String absoluteFieldPath);
}
