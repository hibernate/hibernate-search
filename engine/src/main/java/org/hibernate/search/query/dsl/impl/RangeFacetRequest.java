/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.impl;

import java.util.Date;
import java.util.List;

import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.query.facet.Facet;

/**
 * @author Hardy Ferentschik
 */
public class RangeFacetRequest<T> extends FacetingRequestImpl {
	private final List<FacetRange<T>> facetRangeList;
	private final DocumentBuilderIndexedEntity documentBuilder;

	RangeFacetRequest(String name, String fieldName, List<FacetRange<T>> facetRanges, DocumentBuilderIndexedEntity documentBuilder) {
		super( name, fieldName );
		if ( facetRanges == null || facetRanges.isEmpty() ) {
			throw new IllegalArgumentException( "At least one facet range must be specified" );
		}
		this.facetRangeList = facetRanges;
		this.documentBuilder = documentBuilder;
	}

	public List<FacetRange<T>> getFacetRangeList() {
		return facetRangeList;
	}

	@Override
	public Class<?> getFieldCacheType() {
		// safe since we have at least one facet range set
		Object o = facetRangeList.get( 0 ).getMin();
		if ( o == null ) {
			o = facetRangeList.get( 0 ).getMax();
		}

		if ( o instanceof Date ) { // for date faceting we are using the string field cache
			return String[].class;
		}
		else {
			return o.getClass();
		}
	}

	@Override
	public Facet createFacet(String value, int count) {
		// todo improve implementation. we should not depend on the string value (HF)
		int facetIndex = findFacetRangeIndex( value );
		FacetRange<T> range = facetRangeList.get( facetIndex );
		return new RangeFacetImpl<T>( getFacetingName(), getFieldName(), range, count, facetIndex, documentBuilder );
	}

	@Override
	public String toString() {
		return "RangeFacetRequest{" +
				"facetRangeList=" + facetRangeList +
				"} " + super.toString();
	}

	private int findFacetRangeIndex(String value) {
		int index = 0;
		for ( FacetRange facetRange : facetRangeList ) {
			if ( facetRange.getRangeString().equals( value ) ) {
				return index;
			}
			index++;
		}
		return -1;
	}
}
