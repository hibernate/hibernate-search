/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.TermsAggregationOptionsStep;
import org.hibernate.search.query.facet.Facet;

/**
 * A facet request for string based fields.
 *
 * @author Hardy Ferentschik
 */
public class DiscreteFacetRequest extends FacetingRequestImpl<Map<Object, Long>> {
	DiscreteFacetRequest(String name, String fieldName) {
		super( name, fieldName );
	}

	@Override
	public AggregationFinalStep<Map<Object, Long>> requestAggregation(SearchAggregationFactory factory) {
		TermsAggregationOptionsStep<?, ?, Object, Map<Object, Long>> optionsStep = factory
				.terms().field( getFieldName(), Object.class );
		if ( maxNumberOfFacets >= 0 ) {
			optionsStep = optionsStep.maxTermCount( maxNumberOfFacets );
		}
		if ( includeZeroCounts ) {
			optionsStep.minDocumentCount( 0 );
		}
		switch ( sort ) {
			case COUNT_ASC:
				optionsStep = optionsStep.orderByCountAscending();
				break;
			case COUNT_DESC:
				optionsStep = optionsStep.orderByCountDescending();
				break;
			case FIELD_VALUE:
				optionsStep = optionsStep.orderByTermAscending();
				break;
			case RANGE_DEFINITION_ORDER:
				// Does not make sense; ignore.
				break;
		}
		return optionsStep;
	}

	@Override
	public List<Facet> toFacets(Map<Object, Long> aggregation) {
		List<Facet> result = new ArrayList<>( aggregation.size() );
		for ( Map.Entry<?, Long> entry : aggregation.entrySet() ) {
			String value = String.valueOf( entry.getKey() );
			int count = Math.toIntExact( entry.getValue() );
			result.add( new SimpleFacet( getFacetingName(), getFieldName(), value, count ) );
		}
		return result;
	}

	static class SimpleFacet extends AbstractFacet {
		SimpleFacet(String facetingName, String absoluteFieldPath, String value, int count) {
			super( facetingName, absoluteFieldPath, value, count );
		}
	}
}
