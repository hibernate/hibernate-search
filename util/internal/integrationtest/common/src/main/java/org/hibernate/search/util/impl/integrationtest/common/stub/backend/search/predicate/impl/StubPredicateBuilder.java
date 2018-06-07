/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
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
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubQueryElementCollector;

public class StubPredicateBuilder implements MatchAllPredicateBuilder<StubQueryElementCollector>,
		BooleanJunctionPredicateBuilder<StubQueryElementCollector>,
		MatchPredicateBuilder<StubQueryElementCollector>,
		RangePredicateBuilder<StubQueryElementCollector>,
		NestedPredicateBuilder<StubQueryElementCollector>,
		SpatialWithinCirclePredicateBuilder<StubQueryElementCollector>,
		SpatialWithinPolygonPredicateBuilder<StubQueryElementCollector>,
		SpatialWithinBoundingBoxPredicateBuilder<StubQueryElementCollector> {

	@Override
	public StubQueryElementCollector getMustCollector() {
		return StubQueryElementCollector.get();
	}

	@Override
	public StubQueryElementCollector getMustNotCollector() {
		return StubQueryElementCollector.get();
	}

	@Override
	public StubQueryElementCollector getShouldCollector() {
		return StubQueryElementCollector.get();
	}

	@Override
	public StubQueryElementCollector getFilterCollector() {
		return StubQueryElementCollector.get();
	}

	@Override
	public void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber) {
		// No-op
	}

	@Override
	public void minimumShouldMatchRatio(int ignoreConstraintCeiling, double matchingClausesRatio) {
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
	public StubQueryElementCollector getNestedCollector() {
		return StubQueryElementCollector.get();
	}

	@Override
	public void contribute(StubQueryElementCollector collector) {
		collector.simulateCollectCall();
	}
}
