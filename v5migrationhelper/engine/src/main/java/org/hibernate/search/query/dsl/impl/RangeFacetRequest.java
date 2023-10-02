/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationRangeMoreStep;
import org.hibernate.search.engine.search.aggregation.dsl.RangeAggregationRangeStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.query.engine.impl.FacetComparators;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.util.common.data.Range;

/**
 * @author Hardy Ferentschik
 */
public class RangeFacetRequest<T> extends FacetingRequestImpl<Map<Range<T>, Long>> {
	private final List<FacetRange<T>> facetRangeList;

	RangeFacetRequest(String name, String fieldName, List<FacetRange<T>> facetRanges) {
		super( name, fieldName );
		if ( facetRanges == null || facetRanges.isEmpty() ) {
			throw new IllegalArgumentException( "At least one facet range must be specified" );
		}
		this.facetRangeList = facetRanges;
	}

	@Override
	public AggregationFinalStep<Map<Range<T>, Long>> requestAggregation(SearchAggregationFactory factory) {
		RangeAggregationRangeStep<?, ?, T> rangeStep = factory
				.range().field( getFieldName(), getFacetValueType() );
		RangeAggregationRangeMoreStep<?, ?, ?, T> rangeMoreStep = null;
		for ( FacetRange<T> facetRange : facetRangeList ) {
			rangeMoreStep = rangeStep.range( facetRange.range() );
			rangeStep = rangeMoreStep;
		}
		return rangeMoreStep;
	}

	@Override
	public List<Facet> toFacets(Map<Range<T>, Long> aggregation) {
		List<Facet> result = new ArrayList<>( aggregation.size() );
		for ( Map.Entry<Range<T>, Long> entry : aggregation.entrySet() ) {
			int facetIndex = findFacetRangeIndex( entry.getKey() );
			int count = Math.toIntExact( entry.getValue() );
			if ( count == 0 && !includeZeroCounts ) {
				continue;
			}
			FacetRange<T> range = facetRangeList.get( facetIndex );
			result.add( new RangeFacetImpl<>( getFacetingName(), getFieldName(), range, count ) );
		}
		if ( !sort.equals( FacetSortOrder.RANGE_DEFINITION_ORDER ) ) {
			result.sort( FacetComparators.get( sort ) );
		}
		return result;
	}

	@Override
	public String toString() {
		return "RangeFacetRequest{" +
				"facetRangeList=" + facetRangeList +
				"} " + super.toString();
	}

	private Class<T> getFacetValueType() {
		// safe since we have at least one facet range set
		T o = facetRangeList.get( 0 ).getMin();
		if ( o == null ) {
			o = facetRangeList.get( 0 ).getMax();
		}

		return (Class<T>) o.getClass();
	}

	private int findFacetRangeIndex(Range<T> range) {
		int index = 0;
		for ( FacetRange<T> facetRange : facetRangeList ) {
			if ( facetRange.range().equals( range ) ) {
				return index;
			}
			index++;
		}
		return -1;
	}
}
