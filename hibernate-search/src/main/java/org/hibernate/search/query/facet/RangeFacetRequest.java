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

package org.hibernate.search.query.facet;

import java.util.List;

/**
 * @author Hardy Ferentschik
 */
// todo have some helper method or constructors to create range requests using a start and increment
public class RangeFacetRequest<N extends Number> extends FacetRequest {
	private final List<FacetRange<N>> facetRangeList;

	public RangeFacetRequest(String field, List<FacetRange<N>> facetRanges) {
		this( field, FacetSortOrder.COUNT_DESC, facetRanges );
	}

	public RangeFacetRequest(String fieldName, FacetSortOrder sort, List<FacetRange<N>> facetRanges) {
		this( fieldName, sort, true, facetRanges );
	}

	public RangeFacetRequest(String fieldName, FacetSortOrder sort, boolean includeZeroCounts, List<FacetRange<N>> facetRanges) {
		super( fieldName, sort, includeZeroCounts );
		this.facetRangeList = facetRanges;
	}

	public List<FacetRange<N>> getFacetRangeList() {
		return facetRangeList;
	}

	@Override
	public Class<?> getFieldCacheType() {
		return facetRangeList.get(0).getMin().getClass();
	}

	@Override
	public String toString() {
		return "RangeFacetRequest{" +
				"facetRangeList=" + facetRangeList +
				"} " + super.toString();
	}
}
