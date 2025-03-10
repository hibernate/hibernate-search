package org.hibernate.search.engine.search.reference.spi;

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
import org.hibernate.search.engine.search.reference.predicate.SpatialWithinBoundingBoxPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.SpatialWithinCirclePredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.SpatialWithinPolygonPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.TermsPredicateFieldReference;
import org.hibernate.search.engine.search.reference.predicate.WildcardPredicateFieldReference;
import org.hibernate.search.engine.search.reference.projection.DistanceProjectionFieldReference;
import org.hibernate.search.engine.search.reference.projection.FieldProjectionFieldReference;
import org.hibernate.search.engine.search.reference.projection.HighlightProjectionFieldReference;
import org.hibernate.search.engine.search.reference.projection.ObjectProjectionFieldReference;
import org.hibernate.search.engine.search.reference.sort.DistanceSortFieldReference;
import org.hibernate.search.engine.search.reference.sort.FieldSortFieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public class TraitReferenceMapping {
	private final Map<String, ReferenceDetails> traits;

	private TraitReferenceMapping() {
		Map<String, ReferenceDetails> traits = new HashMap<>( backendSpecificTraits() );

		traits.put( IndexFieldTraits.Predicates.EXISTS, new ReferenceDetails( ExistsPredicateFieldReference.class, "P0" ) );
		traits.put( IndexFieldTraits.Predicates.KNN, new ReferenceDetails( KnnPredicateFieldReference.class, "P1" ) );
		traits.put( IndexFieldTraits.Predicates.MATCH, new ReferenceDetails( MatchPredicateFieldReference.class, "P2" ) );
		traits.put( IndexFieldTraits.Predicates.NESTED, new ReferenceDetails( NestedPredicateFieldReference.class, "P3" ) );
		traits.put( IndexFieldTraits.Predicates.PHRASE, new ReferenceDetails( PhrasePredicateFieldReference.class, "P4" ) );
		traits.put( IndexFieldTraits.Predicates.PREFIX, new ReferenceDetails( PrefixPredicateFieldReference.class, "P5" ) );
		traits.put( IndexFieldTraits.Predicates.RANGE, new ReferenceDetails( RangePredicateFieldReference.class, "P6" ) );
		traits.put( IndexFieldTraits.Predicates.QUERY_STRING,
				new ReferenceDetails( QueryStringPredicateFieldReference.class, "P7" ) );
		traits.put( IndexFieldTraits.Predicates.REGEXP, new ReferenceDetails( RegexpPredicateFieldReference.class, "P8" ) );
		traits.put( IndexFieldTraits.Predicates.SIMPLE_QUERY_STRING,
				new ReferenceDetails( SimpleQueryStringPredicateFieldReference.class, "P9" ) );
		traits.put( IndexFieldTraits.Predicates.SPATIAL_WITHIN_BOUNDING_BOX,
				new ReferenceDetails( SpatialWithinBoundingBoxPredicateFieldReference.class, "P10" ) );
		traits.put( IndexFieldTraits.Predicates.SPATIAL_WITHIN_CIRCLE,
				new ReferenceDetails( SpatialWithinCirclePredicateFieldReference.class, "P11" ) );
		traits.put( IndexFieldTraits.Predicates.SPATIAL_WITHIN_POLYGON,
				new ReferenceDetails( SpatialWithinPolygonPredicateFieldReference.class, "P12" ) );
		traits.put( IndexFieldTraits.Predicates.TERMS, new ReferenceDetails( TermsPredicateFieldReference.class, "P13" ) );
		traits.put( IndexFieldTraits.Predicates.WILDCARD,
				new ReferenceDetails( WildcardPredicateFieldReference.class, "P14" ) );

		traits.put( IndexFieldTraits.Sorts.DISTANCE, new ReferenceDetails( DistanceSortFieldReference.class, "S0" ) );
		traits.put( IndexFieldTraits.Sorts.FIELD, new ReferenceDetails( FieldSortFieldReference.class, "S1" ) );

		traits.put( IndexFieldTraits.Projections.DISTANCE,
				new ReferenceDetails( DistanceProjectionFieldReference.class, "R0" ) );
		traits.put( IndexFieldTraits.Projections.FIELD, new ReferenceDetails( FieldProjectionFieldReference.class, "R1" ) );
		traits.put( IndexFieldTraits.Projections.HIGHLIGHT,
				new ReferenceDetails( HighlightProjectionFieldReference.class, "R2" ) );
		traits.put( IndexFieldTraits.Projections.OBJECT, new ReferenceDetails( ObjectProjectionFieldReference.class, "R3" ) );

		traits.put( IndexFieldTraits.Aggregations.RANGE, new ReferenceDetails( RangeAggregationFieldReference.class, "A0" ) );
		traits.put( IndexFieldTraits.Aggregations.TERMS, new ReferenceDetails( TermsAggregationFieldReference.class, "A1" ) );
		traits.put( IndexFieldTraits.Aggregations.SUM, new ReferenceDetails( SumAggregationFieldReference.class, "A2" ) );
		traits.put( IndexFieldTraits.Aggregations.MIN, new ReferenceDetails( MinAggregationFieldReference.class, "A3" ) );
		traits.put( IndexFieldTraits.Aggregations.MAX, new ReferenceDetails( MaxAggregationFieldReference.class, "A4" ) );
		traits.put( IndexFieldTraits.Aggregations.COUNT, new ReferenceDetails( CountAggregationFieldReference.class, "A5" ) );
		traits.put( IndexFieldTraits.Aggregations.COUNT_DISTINCT,
				new ReferenceDetails( CountDistinctAggregationFieldReference.class, "A6" ) );
		traits.put( IndexFieldTraits.Aggregations.AVG, new ReferenceDetails( AvgAggregationFieldReference.class, "A7" ) );

		this.traits = Collections.unmodifiableMap( traits );
	}

	protected Map<String, ReferenceDetails> backendSpecificTraits() {
		return Map.of();
	}

	public static TraitReferenceMapping instance() {
		return new TraitReferenceMapping();
	}

	public ReferenceDetails reference(String traitName) {
		return traits.get( traitName );
	}

	public record ReferenceDetails(Class<?> referenceClass, String implementationLabel) {
	}

}
