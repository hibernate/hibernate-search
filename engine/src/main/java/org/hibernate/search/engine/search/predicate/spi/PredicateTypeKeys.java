/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class PredicateTypeKeys {

	private PredicateTypeKeys() {
	}

	public static <T> SearchQueryElementTypeKey<T> key(String name) {
		return SearchQueryElementTypeKey.of( "predicate", name );
	}

	public static SearchQueryElementTypeKey<NamedPredicateBuilder> named(String name) {
		return key( "named:" + name );
	}

	public static final SearchQueryElementTypeKey<NestedPredicateBuilder> NESTED = key( "nested" );
	public static final SearchQueryElementTypeKey<MatchPredicateBuilder> MATCH = key( "match" );
	public static final SearchQueryElementTypeKey<RangePredicateBuilder> RANGE = key( "range" );
	public static final SearchQueryElementTypeKey<ExistsPredicateBuilder> EXISTS = key( "exists" );
	public static final SearchQueryElementTypeKey<PhrasePredicateBuilder> PHRASE = key( "phrase" );
	public static final SearchQueryElementTypeKey<WildcardPredicateBuilder> WILDCARD = key( "wildcard" );
	public static final SearchQueryElementTypeKey<RegexpPredicateBuilder> REGEXP = key( "regexp" );
	public static final SearchQueryElementTypeKey<TermsPredicateBuilder> TERMS = key( "terms" );
	public static final SearchQueryElementTypeKey<SpatialWithinCirclePredicateBuilder> SPATIAL_WITHIN_CIRCLE =
			key( "spatial:within-circle" );
	public static final SearchQueryElementTypeKey<SpatialWithinPolygonPredicateBuilder> SPATIAL_WITHIN_POLYGON =
			key( "spatial:within-polygon" );
	public static final SearchQueryElementTypeKey<SpatialWithinBoundingBoxPredicateBuilder> SPATIAL_WITHIN_BOUNDING_BOX =
			key( "spatial:within-bounding-box" );
	public static final SearchQueryElementTypeKey<KnnPredicateBuilder> KNN = key( "knn" );

}
