/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl;

import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubQueryElementCollector;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.BooleanJunctionPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateContributor;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;

public class StubSearchPredicateFactory implements SearchPredicateFactory<Void, StubQueryElementCollector> {

	@Override
	public Void createRootContext() {
		return null;
	}

	@Override
	public SearchPredicate toSearchPredicate(SearchPredicateContributor<Void, ? super StubQueryElementCollector> contributor) {
		contributor.contribute( null, StubQueryElementCollector.get() );
		return new StubSearchPredicate();
	}

	@Override
	public SearchPredicateContributor<Void, StubQueryElementCollector> toContributor(SearchPredicate predicate) {
		return (StubSearchPredicate) predicate;
	}

	@Override
	public MatchAllPredicateBuilder<Void, StubQueryElementCollector> matchAll() {
		return new StubPredicateBuilder();
	}

	@Override
	public BooleanJunctionPredicateBuilder<Void, StubQueryElementCollector> bool() {
		return new StubPredicateBuilder();
	}

	@Override
	public MatchPredicateBuilder<Void, StubQueryElementCollector> match(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public RangePredicateBuilder<Void, StubQueryElementCollector> range(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public SpatialWithinCirclePredicateBuilder<Void, StubQueryElementCollector> spatialWithinCircle(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder<Void, StubQueryElementCollector> spatialWithinPolygon(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder<Void, StubQueryElementCollector> spatialWithinBoundingBox(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public NestedPredicateBuilder<Void, StubQueryElementCollector> nested(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}
}
