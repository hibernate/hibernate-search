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
import org.hibernate.search.engine.search.predicate.spi.NamedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.NestedPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.predicate.spi.SimpleQueryStringPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.TermsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.query.impl.StubQueryElementCollector;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl.StubSearchIndexScope;

public class StubSearchPredicateBuilderFactory
		implements SearchPredicateBuilderFactory<StubQueryElementCollector> {

	private final StubSearchIndexScope scope;

	public StubSearchPredicateBuilderFactory(StubSearchIndexScope scope) {
		this.scope = scope;
	}

	@Override
	public void contribute(StubQueryElementCollector collector, SearchPredicate predicate) {
		( (StubSearchPredicate) predicate ).simulateBuild();
		collector.simulateCollectCall();
	}

	@Override
	public MatchIdPredicateBuilder id() {
		return new StubSearchPredicate.Builder();
	}

	@Override
	public MatchAllPredicateBuilder matchAll() {
		return new StubSearchPredicate.Builder();
	}

	@Override
	public BooleanPredicateBuilder bool() {
		return new StubSearchPredicate.Builder();
	}

	@Override
	public MatchPredicateBuilder match(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.MATCH, scope );
	}

	@Override
	public RangePredicateBuilder range(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.RANGE, scope );
	}

	@Override
	public PhrasePredicateBuilder phrase(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.PHRASE, scope );
	}

	@Override
	public WildcardPredicateBuilder wildcard(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.WILDCARD, scope );
	}

	@Override
	public RegexpPredicateBuilder regexp(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.REGEXP, scope );
	}

	@Override
	public TermsPredicateBuilder terms(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.TERMS, scope );
	}

	@Override
	public SimpleQueryStringPredicateBuilder simpleQueryString() {
		return new StubSearchPredicate.Builder();
	}

	@Override
	public ExistsPredicateBuilder exists(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.EXISTS, scope );
	}

	@Override
	public SpatialWithinCirclePredicateBuilder spatialWithinCircle(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.SPATIAL_WITHIN_CIRCLE, scope );
	}

	@Override
	public SpatialWithinPolygonPredicateBuilder spatialWithinPolygon(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.SPATIAL_WITHIN_POLYGON, scope );
	}

	@Override
	public SpatialWithinBoundingBoxPredicateBuilder spatialWithinBoundingBox(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.SPATIAL_WITHIN_BOUNDING_BOX, scope );
	}

	@Override
	public NestedPredicateBuilder nested(String absoluteFieldPath) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.NESTED, scope );
	}

	@Override
	public NamedPredicateBuilder named(String absoluteFieldPath, String name) {
		return scope.field( absoluteFieldPath ).queryElement( PredicateTypeKeys.named( name ), scope );
	}
}
