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

import java.util.List;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetRange;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 */
// todo have some helper method or constructors to create range requests using a start and increment
public class RangeFacetRequest<T> extends FacetingRequest {
	private final List<FacetRange<T>> facetRangeList;

	RangeFacetRequest(String name, String fieldName, List<FacetRange<T>> facetRanges) {
		super( name, fieldName );
		if ( facetRanges == null || facetRanges.isEmpty() ) {
			throw new IllegalArgumentException( "At least one facet range must be specified" );
		}
		this.facetRangeList = facetRanges;
	}

	public List<FacetRange<T>> getFacetRangeList() {
		return facetRangeList;
	}

	@Override
	public Class<?> getFieldCacheType() {
		// safe since we have at least one facet range
		return facetRangeList.get( 0 ).getMin().getClass();
	}

	@Override
	public Facet createFacet(String value, int count) {
		// todo improve implementation. we should not depend on the string value (HF)
		FacetRange<T> range = findFacetRange( value );
		return new RangeFacet<T>( getFieldName(), range, count );
	}

	@Override
	public String toString() {
		return "RangeFacetRequest{" +
				"facetRangeList=" + facetRangeList +
				"} " + super.toString();
	}

	private FacetRange<T> findFacetRange(String value) {
		FacetRange<T> range = null;
		for ( FacetRange<T> facetRange : facetRangeList ) {
			if ( facetRange.getRangeString().equals( value ) ) {
				range = facetRange;
			}
		}
		if ( range == null ) {
			throw new AssertionFailure( "There should have been a matching facet range" );
		}
		return range;
	}

	static class RangeFacet<T> extends Facet {
		private FacetRange<T> range;

		RangeFacet(String fieldName, FacetRange<T> range, int count) {
			super( fieldName, range.getRangeString(), count );
			this.range = range;
		}

		@Override
		public Query getFacetQuery() {
			if ( range.getMin() instanceof Number ) {
				return createNumericRangeQuery();
			}
			else {
				throw new AssertionFailure( "Unsupported range type" );
			}
		}

		private Query createNumericRangeQuery() {
			NumericRangeQuery query;
			if ( range.getMin() instanceof Double ) {
				query = NumericRangeQuery.newDoubleRange(
						getFieldName(),
						(Double) range.getMin(),
						(Double) range.getMax(),
						range.isIncludeMin(),
						range.isIncludeMax()
				);
			}
			else if ( range.getMin() instanceof Float ) {
				query = NumericRangeQuery.newFloatRange(
						getFieldName(),
						(Float) range.getMin(),
						(Float) range.getMax(),
						range.isIncludeMin(),
						range.isIncludeMax()
				);
			}
			else if ( range.getMin() instanceof Integer ) {
				query = NumericRangeQuery.newIntRange(
						getFieldName(),
						(Integer) range.getMin(),
						(Integer) range.getMax(),
						range.isIncludeMin(),
						range.isIncludeMax()
				);
			}

			else if ( range.getMin() instanceof Long ) {
				query = NumericRangeQuery.newLongRange(
						getFieldName(),
						(Long) range.getMin(),
						(Long) range.getMax(),
						range.isIncludeMin(),
						range.isIncludeMax()
				);
			}
			else {
				throw new AssertionFailure( "Unsupported range type" );
			}
			return query;
		}
	}
}
