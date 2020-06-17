/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.predicate.impl;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.BooleanPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchAllPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchIdPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubQueryElementCollector;

public class StubSearchPredicateBuilderFactory
		implements SearchPredicateBuilderFactory<StubQueryElementCollector> {

	@Override
	public void contribute(StubQueryElementCollector collector, SearchPredicate predicate) {
		( (StubSearchPredicate) predicate ).simulateBuild();
		collector.simulateCollectCall();
	}

	@Override
	public MatchIdPredicateBuilder id() {
		return new StubPredicateBuilder();
	}

	@Override
	public MatchAllPredicateBuilder matchAll() {
		return new StubPredicateBuilder();
	}

	@Override
	public BooleanPredicateBuilder bool() {
		return new StubPredicateBuilder();
	}

	@Override
	public MatchPredicateBuilder match(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public RangePredicateBuilder range(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public PhrasePredicateBuilder phrase(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public WildcardPredicateBuilder wildcard(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public SimpleQueryStringPredicateBuilder simpleQueryString() {
		return new StubPredicateBuilder();
	}

	@Override
	public ExistsPredicateBuilder exists(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public SpatialWithinCirclePredicateBuilder spatialWithinCircle(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder spatialWithinPolygon(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder spatialWithinBoundingBox(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}

	@Override
	public NestedPredicateBuilder nested(String absoluteFieldPath) {
		return new StubPredicateBuilder();
	}
}
