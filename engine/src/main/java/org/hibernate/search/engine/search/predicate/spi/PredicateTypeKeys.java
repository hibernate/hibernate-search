/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import static org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey.of;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;

public final class PredicateTypeKeys {

	private PredicateTypeKeys() {
	}

	public static <T> SearchQueryElementTypeKey<T> key(String name) {
		return of( name );
	}

	public static SearchQueryElementTypeKey<NamedPredicateBuilder> named(String name) {
		return of( IndexFieldTraits.Predicates.named( name ) );
	}

	public static final SearchQueryElementTypeKey<NestedPredicateBuilder> NESTED = of( IndexFieldTraits.Predicates.NESTED );
	public static final SearchQueryElementTypeKey<MatchPredicateBuilder> MATCH = of( IndexFieldTraits.Predicates.MATCH );
	public static final SearchQueryElementTypeKey<RangePredicateBuilder> RANGE = of( IndexFieldTraits.Predicates.RANGE );
	public static final SearchQueryElementTypeKey<ExistsPredicateBuilder> EXISTS = of( IndexFieldTraits.Predicates.EXISTS );
	public static final SearchQueryElementTypeKey<PhrasePredicateBuilder> PHRASE = of( IndexFieldTraits.Predicates.PHRASE );
	public static final SearchQueryElementTypeKey<WildcardPredicateBuilder> WILDCARD =
			of( IndexFieldTraits.Predicates.WILDCARD );
	public static final SearchQueryElementTypeKey<RegexpPredicateBuilder> REGEXP = of( IndexFieldTraits.Predicates.REGEXP );
	public static final SearchQueryElementTypeKey<TermsPredicateBuilder> TERMS = of( IndexFieldTraits.Predicates.TERMS );
	public static final SearchQueryElementTypeKey<SpatialWithinCirclePredicateBuilder> SPATIAL_WITHIN_CIRCLE =
			of( IndexFieldTraits.Predicates.SPATIAL_WITHIN_CIRCLE );
	public static final SearchQueryElementTypeKey<SpatialWithinPolygonPredicateBuilder> SPATIAL_WITHIN_POLYGON =
			of( IndexFieldTraits.Predicates.SPATIAL_WITHIN_POLYGON );
	public static final SearchQueryElementTypeKey<SpatialWithinBoundingBoxPredicateBuilder> SPATIAL_WITHIN_BOUNDING_BOX =
			of( IndexFieldTraits.Predicates.SPATIAL_WITHIN_BOUNDING_BOX );
	public static final SearchQueryElementTypeKey<KnnPredicateBuilder> KNN = of( IndexFieldTraits.Predicates.KNN );

}
