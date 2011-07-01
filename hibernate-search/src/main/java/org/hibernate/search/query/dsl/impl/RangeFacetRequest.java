/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	private final DocumentBuilderIndexedEntity<?> documentBuilder;

	RangeFacetRequest(String name, String fieldName, List<FacetRange<T>> facetRanges, DocumentBuilderIndexedEntity<?> documentBuilder) {
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
			return String.class;
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
		for ( FacetRange<T> facetRange : facetRangeList ) {
			if ( facetRange.getRangeString().equals( value ) ) {
				return index;
			}
			index++;
		}
		return -1;
	}
}
