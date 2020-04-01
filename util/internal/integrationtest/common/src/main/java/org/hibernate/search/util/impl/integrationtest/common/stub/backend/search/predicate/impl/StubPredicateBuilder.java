/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl;

import java.util.Set;
import org.hibernate.search.engine.search.common.BooleanOperator;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.predicate.dsl.SimpleQueryFlag;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.FilterPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.util.common.data.Range;

public class StubPredicateBuilder implements MatchAllPredicateBuilder<StubPredicateBuilder>,
	BooleanPredicateBuilder<StubPredicateBuilder>,
	MatchIdPredicateBuilder<StubPredicateBuilder>,
	MatchPredicateBuilder<StubPredicateBuilder>,
	RangePredicateBuilder<StubPredicateBuilder>,
	PhrasePredicateBuilder<StubPredicateBuilder>,
	WildcardPredicateBuilder<StubPredicateBuilder>,
	SimpleQueryStringPredicateBuilder<StubPredicateBuilder>,
	NestedPredicateBuilder<StubPredicateBuilder>,
	ExistsPredicateBuilder<StubPredicateBuilder>,
	SpatialWithinCirclePredicateBuilder<StubPredicateBuilder>,
	SpatialWithinPolygonPredicateBuilder<StubPredicateBuilder>,
	SpatialWithinBoundingBoxPredicateBuilder<StubPredicateBuilder>,
	FilterPredicateBuilder<StubPredicateBuilder> {

	@Override
	public StubPredicateBuilder toImplementation() {
		return this;
	}

	@Override
	public void must(StubPredicateBuilder clauseBuilder) {
		// No-op
	}

	@Override
	public void mustNot(StubPredicateBuilder clauseBuilder) {
		// No-op
	}

	@Override
	public void should(StubPredicateBuilder clauseBuilder) {
		// No-op

	}

	@Override
	public void filter(StubPredicateBuilder clauseBuilder) {
		// No-op
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
	public void pattern(String wildcardPattern) {
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
	public void nested(StubPredicateBuilder clauseBuilder) {
		// No-op
	}

	@Override
	public void param(String name, Object value) {
		// No-op, just simulates a call on this object
	}

	void simulateBuild() {
		// No-op, just simulates a call on this object
	}

	@Override
	public void flags(Set<SimpleQueryFlag> flags) {
		// No-op, just simulates a call on this object
	}

	private static class StubFieldState implements FieldState {

		@Override
		public void boost(float boost) {
			// No-op
		}
	}

}
