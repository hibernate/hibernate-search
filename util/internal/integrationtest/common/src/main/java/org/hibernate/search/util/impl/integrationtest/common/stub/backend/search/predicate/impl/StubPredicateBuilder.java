/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;

public class StubPredicateBuilder implements MatchAllPredicateBuilder<StubPredicateBuilder>,
		BooleanJunctionPredicateBuilder<StubPredicateBuilder>,
		MatchIdPredicateBuilder<StubPredicateBuilder>,
		MatchPredicateBuilder<StubPredicateBuilder>,
		RangePredicateBuilder<StubPredicateBuilder>,
		NestedPredicateBuilder<StubPredicateBuilder>,
		SpatialWithinCirclePredicateBuilder<StubPredicateBuilder>,
		SpatialWithinPolygonPredicateBuilder<StubPredicateBuilder>,
		SpatialWithinBoundingBoxPredicateBuilder<StubPredicateBuilder> {

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
	public void value(Object value) {
		// No-op
	}

	@Override
	public void lowerLimit(Object value) {
		// No-op
	}

	@Override
	public void excludeLowerLimit() {
		// No-op
	}

	@Override
	public void upperLimit(Object value) {
		// No-op
	}

	@Override
	public void excludeUpperLimit() {
		// No-op
	}

	@Override
	public void boost(float boost) {
		// No-op
	}

	@Override
	public void withConstantScore() {
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

	void simulateBuild() {
		// No-op, just simulates a call on this object
	}

}
