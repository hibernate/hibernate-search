/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.writer.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.engine.backend.types.IndexFieldTraits;
import org.hibernate.search.engine.search.reference.aggregation.AvgAggregationFieldReference;
import org.hibernate.search.engine.search.reference.aggregation.CountAggregationFieldReference;
import org.hibernate.search.engine.search.reference.aggregation.CountDistinctAggregationFieldReference;
import org.hibernate.search.engine.search.reference.aggregation.MaxAggregationFieldReference;
import org.hibernate.search.engine.search.reference.aggregation.MinAggregationFieldReference;
import org.hibernate.search.engine.search.reference.aggregation.RangeAggregationFieldReference;
import org.hibernate.search.engine.search.reference.aggregation.SumAggregationFieldReference;
import org.hibernate.search.engine.search.reference.aggregation.TermsAggregationFieldReference;
import org.hibernate.search.engine.search.reference.predicate.ExistsPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.KnnPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.MatchPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.NestedPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.PhrasePredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.PrefixPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.QueryStringPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.RangePredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.RegexpPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.SimpleQueryStringPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.SpatialPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.TermsPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.WildcardPredicateFieldReference;
import org.hibernate.search.engine.search.reference.projection.DistanceProjectionFieldReference;
import org.hibernate.search.engine.search.reference.projection.FieldProjectionFieldReference;
import org.hibernate.search.engine.search.reference.projection.HighlightProjectionFieldReference;
import org.hibernate.search.engine.search.reference.projection.ObjectProjectionFieldReference;
import org.hibernate.search.engine.search.reference.sort.DistanceSortFieldReference;
import org.hibernate.search.engine.search.reference.sort.FieldSortFieldReference;

class TraitReferenceMapping {
	private static final String EXTRA_PROPERTY_PREDICATE_TYPE = "predicateType";
	private static final String EXTRA_PROPERTY_AGGREGATION_TYPE = "aggregationType";
	private static final String EXTRA_PROPERTY_PROJECTION_TYPE = "projectionType";
	private static final String EXTRA_PROPERTY_SORT_TYPE = "sortType";
	private final Map<String, TraitReferenceDetails> traits;

	private TraitReferenceMapping() {
		Map<String, TraitReferenceDetails> traits = new HashMap<>( backendSpecificTraits() );

		traits.put( IndexFieldTraits.Predicates.EXISTS,
				new TraitReferenceDetails( ExistsPredicateFieldReference.class, "P0", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Predicates.KNN,
				new TraitReferenceDetails( KnnPredicateFieldReference.class, "P1", TraitKind.TYPED_INPUT,
						EXTRA_PROPERTY_PREDICATE_TYPE ) );
		traits.put( IndexFieldTraits.Predicates.MATCH,
				new TraitReferenceDetails( MatchPredicateFieldReference.class, "P2", TraitKind.TYPED_INPUT,
						EXTRA_PROPERTY_PREDICATE_TYPE ) );
		traits.put( IndexFieldTraits.Predicates.NESTED,
				new TraitReferenceDetails( NestedPredicateFieldReference.class, "P3", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Predicates.PHRASE, new TraitReferenceDetails( PhrasePredicateFieldReference.class, "P4",
				TraitKind.TYPED_INPUT, EXTRA_PROPERTY_PREDICATE_TYPE
		) );
		traits.put( IndexFieldTraits.Predicates.PREFIX,
				new TraitReferenceDetails( PrefixPredicateFieldReference.class, "P5", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Predicates.RANGE,
				new TraitReferenceDetails( RangePredicateFieldReference.class, "P6", TraitKind.TYPED_INPUT,
						EXTRA_PROPERTY_PREDICATE_TYPE ) );
		traits.put( IndexFieldTraits.Predicates.QUERY_STRING, new TraitReferenceDetails(
				QueryStringPredicateFieldReference.class, "P7", TraitKind.TYPED_INPUT, EXTRA_PROPERTY_PREDICATE_TYPE ) );
		traits.put( IndexFieldTraits.Predicates.REGEXP,
				new TraitReferenceDetails( RegexpPredicateFieldReference.class, "P8", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Predicates.SIMPLE_QUERY_STRING, new TraitReferenceDetails(
				SimpleQueryStringPredicateFieldReference.class, "P9", TraitKind.TYPED_INPUT, EXTRA_PROPERTY_PREDICATE_TYPE ) );
		traits.put( IndexFieldTraits.Predicates.SPATIAL_WITHIN_BOUNDING_BOX,
				new TraitReferenceDetails( SpatialPredicateFieldReference.class, "P10", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Predicates.SPATIAL_WITHIN_CIRCLE,
				new TraitReferenceDetails( SpatialPredicateFieldReference.class, "P10", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Predicates.SPATIAL_WITHIN_POLYGON,
				new TraitReferenceDetails( SpatialPredicateFieldReference.class, "P10", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Predicates.TERMS,
				new TraitReferenceDetails( TermsPredicateFieldReference.class, "P13", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Predicates.WILDCARD,
				new TraitReferenceDetails( WildcardPredicateFieldReference.class, "P14", TraitKind.UNTYPED ) );

		traits.put( IndexFieldTraits.Sorts.DISTANCE,
				new TraitReferenceDetails( DistanceSortFieldReference.class, "S0", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Sorts.FIELD,
				new TraitReferenceDetails( FieldSortFieldReference.class, "S1", TraitKind.TYPED_INPUT,
						EXTRA_PROPERTY_SORT_TYPE ) );


		traits.put( IndexFieldTraits.Projections.DISTANCE,
				new TraitReferenceDetails( DistanceProjectionFieldReference.class, "R0", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Projections.FIELD, new TraitReferenceDetails( FieldProjectionFieldReference.class, "R1",
				TraitKind.TYPED_OUTPUT, EXTRA_PROPERTY_PROJECTION_TYPE
		) );
		traits.put( IndexFieldTraits.Projections.HIGHLIGHT,
				new TraitReferenceDetails( HighlightProjectionFieldReference.class, "R2", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Projections.OBJECT,
				new TraitReferenceDetails( ObjectProjectionFieldReference.class, "R3", TraitKind.UNTYPED ) );

		// TODO, can we really use output for the range agg ?
		//  if not we should probably consider having different agg types, one that match inputs and other oputputs with different .aggregationType() in them.
		traits.put( IndexFieldTraits.Aggregations.RANGE, new TraitReferenceDetails( RangeAggregationFieldReference.class, "A0",
				TraitKind.TYPED_OUTPUT, EXTRA_PROPERTY_AGGREGATION_TYPE
		) );
		traits.put( IndexFieldTraits.Aggregations.TERMS, new TraitReferenceDetails( TermsAggregationFieldReference.class, "A1",
				TraitKind.TYPED_OUTPUT, EXTRA_PROPERTY_AGGREGATION_TYPE
		) );
		traits.put( IndexFieldTraits.Aggregations.SUM, new TraitReferenceDetails( SumAggregationFieldReference.class, "A2",
				TraitKind.TYPED_OUTPUT, EXTRA_PROPERTY_AGGREGATION_TYPE
		) );
		traits.put( IndexFieldTraits.Aggregations.MIN, new TraitReferenceDetails( MinAggregationFieldReference.class, "A3",
				TraitKind.TYPED_OUTPUT, EXTRA_PROPERTY_AGGREGATION_TYPE
		) );
		traits.put( IndexFieldTraits.Aggregations.MAX, new TraitReferenceDetails( MaxAggregationFieldReference.class, "A4",
				TraitKind.TYPED_OUTPUT, EXTRA_PROPERTY_AGGREGATION_TYPE
		) );
		traits.put( IndexFieldTraits.Aggregations.COUNT,
				new TraitReferenceDetails( CountAggregationFieldReference.class, "A5", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Aggregations.COUNT_DISTINCT,
				new TraitReferenceDetails( CountDistinctAggregationFieldReference.class, "A6", TraitKind.UNTYPED ) );
		traits.put( IndexFieldTraits.Aggregations.AVG, new TraitReferenceDetails( AvgAggregationFieldReference.class, "A7",
				TraitKind.TYPED_OUTPUT, EXTRA_PROPERTY_AGGREGATION_TYPE
		) );

		this.traits = Collections.unmodifiableMap( traits );
	}

	protected Map<String, TraitReferenceDetails> backendSpecificTraits() {
		return Map.of();
	}

	public static TraitReferenceMapping instance() {
		return new TraitReferenceMapping();
	}

	public TraitReferenceDetails reference(String traitName) {
		return traits.get( traitName );
	}

}
