/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import org.hibernate.search.backend.lucene.search.impl.SearchQueryElementTypeKey;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneSimpleQueryStringPredicateBuilderFieldState;
import org.hibernate.search.engine.search.predicate.spi.ExistsPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.MatchPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.PhrasePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RangePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.RegexpPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinBoundingBoxPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinCirclePredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.SpatialWithinPolygonPredicateBuilder;
import org.hibernate.search.engine.search.predicate.spi.WildcardPredicateBuilder;

public final class PredicateTypeKeys {

	private PredicateTypeKeys() {
	}

	public static final SearchQueryElementTypeKey<MatchPredicateBuilder> MATCH = key( "match" );
	public static final SearchQueryElementTypeKey<RangePredicateBuilder> RANGE = key( "range" );
	public static final SearchQueryElementTypeKey<ExistsPredicateBuilder> EXISTS = key( "exists" );
	public static final SearchQueryElementTypeKey<PhrasePredicateBuilder> PHRASE = key( "phrase" );
	public static final SearchQueryElementTypeKey<WildcardPredicateBuilder> WILDCARD = key( "wildcard" );
	public static final SearchQueryElementTypeKey<RegexpPredicateBuilder> REGEXP = key( "regexp" );
	public static final SearchQueryElementTypeKey<LuceneSimpleQueryStringPredicateBuilderFieldState> SIMPLE_QUERY_STRING =
			key( "simple-query-string" );
	public static final SearchQueryElementTypeKey<SpatialWithinCirclePredicateBuilder> SPATIAL_WITHIN_CIRCLE =
			key( "spatial:within-circle" );
	public static final SearchQueryElementTypeKey<SpatialWithinPolygonPredicateBuilder> SPATIAL_WITHIN_POLYGON =
			key( "spatial:within-polygon" );
	public static final SearchQueryElementTypeKey<SpatialWithinBoundingBoxPredicateBuilder> SPATIAL_WITHIN_BOUNDING_BOX =
			key( "spatial:within-bounding-box" );

	private static <T> SearchQueryElementTypeKey<T> key(String name) {
		return SearchQueryElementTypeKey.of( "predicate", name );
	}

}
