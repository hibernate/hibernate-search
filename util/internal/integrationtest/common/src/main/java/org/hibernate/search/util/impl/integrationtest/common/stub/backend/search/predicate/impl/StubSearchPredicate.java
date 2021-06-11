/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl;

import java.util.Collection;
import java.util.Set;

import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.TermsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexSchemaElementContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchQueryElementFactory;

public class StubSearchPredicate implements SearchPredicate {

	public static StubSearchPredicate from(SearchPredicate predicate) {
		return (StubSearchPredicate) predicate;
	}

	private StubSearchPredicate() {
	}

	void simulateBuild() {
		// No-op, just simulates a call on this object
	}

	public static class Factory implements StubSearchQueryElementFactory<Builder> {
		@Override
		public Builder create(StubSearchIndexScope scope, StubSearchIndexSchemaElementContext element) {
			return new Builder();
		}
	}

	public static class Builder implements MatchAllPredicateBuilder,
			BooleanPredicateBuilder,
			MatchIdPredicateBuilder,
			MatchPredicateBuilder,
			RangePredicateBuilder,
			PhrasePredicateBuilder,
			WildcardPredicateBuilder,
			RegexpPredicateBuilder,
			TermsPredicateBuilder,
			SimpleQueryStringPredicateBuilder,
			NestedPredicateBuilder,
			ExistsPredicateBuilder,
			SpatialWithinCirclePredicateBuilder,
			SpatialWithinPolygonPredicateBuilder,
			SpatialWithinBoundingBoxPredicateBuilder,
			NamedPredicateBuilder {

		@Override
		public SearchPredicate build() {
			return new StubSearchPredicate();
		}

		@Override
		public void must(SearchPredicate clause) {
			// No-op, just check the type
			from( clause );
		}

		@Override
		public void mustNot(SearchPredicate clause) {
			// No-op, just check the type
			from( clause );
		}

		@Override
		public void should(SearchPredicate clause) {
			// No-op, just check the type
			from( clause );

		}

		@Override
		public void filter(SearchPredicate clause) {
			// No-op, just check the type
			from( clause );
		}

		@Override
		public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
			// No-op
		}

		@Override
		public void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent) {
			// No-op
		}

		@Override
		public void fuzzy(int maxEditDistance, int exactPrefixLength) {
			// No-op
		}

		@Override
		public void value(Object value, ValueConvert convert) {
			// No-op
		}

		@Override
		public void analyzer(String analyzerName) {
			// No-op
		}

		@Override
		public void skipAnalysis() {
			// No-op
		}

		@Override
		public void range(Range<?> range, ValueConvert convertLowerBound, ValueConvert convertUpperBound) {
			// No-op
		}

		@Override
		public void slop(int slop) {
			// No-op
		}

		@Override
		public void phrase(String phrase) {
			// No-op
		}

		@Override
		public void pattern(String pattern) {
			// used by both wildcard and regexp predicate
			// No-op
		}

		@Override
		public void matchingAny(Collection<?> terms, ValueConvert convert) {
			// No-op
		}

		@Override
		public void matchingAll(Collection<?> terms, ValueConvert convert) {
			// No-op
		}

		@Override
		public FieldState field(String absoluteFieldPath) {
			return new StubFieldState();
		}

		@Override
		public void defaultOperator(BooleanOperator operator) {
			// No-op
		}

		@Override
		public void simpleQueryString(String simpleQueryString) {
			// No-op
		}

		@Override
		public void boost(float boost) {
			// No-op
		}

		@Override
		public void constantScore() {
			// No-op
		}

		@Override
		public void circle(GeoPoint center, double radius, DistanceUnit unit) {
			// No-op
		}

		@Override
		public void polygon(GeoPolygon polygon) {
			// No-op
		}

		@Override
		public void boundingBox(GeoBoundingBox boundingBox) {
			// No-op
		}

		@Override
		public void nested(SearchPredicate nestedPredicate) {
			// No-op, just check the type
			from( nestedPredicate );
		}

		@Override
		public void factory(SearchPredicateFactory factory) {
			// No-op, just simulates a call on this object
		}

		@Override
		public void param(String name, Object value) {
			// No-op, just simulates a call on this object
		}

		@Override
		public void flags(Set<SimpleQueryFlag> flags) {
			// No-op, just simulates a call on this object
		}
	}

	private static class StubFieldState implements SimpleQueryStringPredicateBuilder.FieldState {

		@Override
		public void boost(float boost) {
			// No-op
		}
	}
}
