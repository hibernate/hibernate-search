/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoBoundingBox;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.GeoPolygon;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubQueryElementCollector;

public class StubPredicateBuilder implements MatchAllPredicateBuilder<Void, StubQueryElementCollector>,
		BooleanJunctionPredicateBuilder<Void, StubQueryElementCollector>,
		MatchPredicateBuilder<Void, StubQueryElementCollector>,
		RangePredicateBuilder<Void, StubQueryElementCollector>,
		NestedPredicateBuilder<Void, StubQueryElementCollector>,
		SpatialWithinCirclePredicateBuilder<Void, StubQueryElementCollector>,
		SpatialWithinPolygonPredicateBuilder<Void, StubQueryElementCollector>,
		SpatialWithinBoundingBoxPredicateBuilder<Void, StubQueryElementCollector> {

	@Override
	public Consumer<SearchPredicateContributor<Void, ? super StubQueryElementCollector>> getMustCollector() {
		return this::consumeBooleanContributor;
	}

	@Override
	public Consumer<SearchPredicateContributor<Void, ? super StubQueryElementCollector>> getMustNotCollector() {
		return this::consumeBooleanContributor;
	}

	@Override
	public Consumer<SearchPredicateContributor<Void, ? super StubQueryElementCollector>> getShouldCollector() {
		return this::consumeBooleanContributor;
	}

	@Override
	public Consumer<SearchPredicateContributor<Void, ? super StubQueryElementCollector>> getFilterCollector() {
		return this::consumeBooleanContributor;
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
	public Consumer<SearchPredicateContributor<Void, ? super StubQueryElementCollector>> getNestedCollector() {
		return this::consumeNestedContributor;
	}

	@Override
	public void contribute(Void context, StubQueryElementCollector collector) {
		collector.simulateCollectCall();
	}

	private void consumeNestedContributor(SearchPredicateContributor<Void, ? super StubQueryElementCollector> nestedContributor) {
	}

	private void consumeBooleanContributor(SearchPredicateContributor<Void, ? super StubQueryElementCollector> booleanContributor) {
	}
}
