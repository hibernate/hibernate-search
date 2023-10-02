/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;
import org.hibernate.search.util.StringHelper;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Hardy Ferentschik
 */
class FacetBuildingContext<T> {
	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	/**
	 * The list of types which are supported for range faceting
	 */
	private static final List<String> allowedRangeTypes = Arrays.asList(
			String.class.getName(),
			Integer.class.getName(),
			Long.class.getName(),
			Double.class.getName(),
			Float.class.getName(),
			Date.class.getName()
	);

	private final QueryBuildingContext context;

	private String name;
	private String fieldName;
	private FacetSortOrder sort = FacetSortOrder.COUNT_DESC;
	private boolean includeZeroCount = false;
	private boolean isRangeQuery = false;
	private List<FacetRange<T>> rangeList = new ArrayList<>();
	private T rangeStart;
	private boolean includeRangeStart = true;
	private T rangeEnd;
	private boolean includeRangeEnd = true;
	private int maxFacetCount = -1;

	public FacetBuildingContext(QueryBuildingContext context) {
		this.context = context;
	}

	void setName(String name) {
		this.name = name;
	}

	void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	void setSort(FacetSortOrder sort) {
		this.sort = sort;
	}

	void setIncludeZeroCount(boolean includeZeroCount) {
		this.includeZeroCount = includeZeroCount;
	}

	public void setRangeQuery(boolean rangeQuery) {
		isRangeQuery = rangeQuery;
	}

	public void setRangeStart(T rangeStart) {
		this.rangeStart = rangeStart;
	}

	public void setIncludeRangeStart(boolean includeRangeStart) {
		this.includeRangeStart = includeRangeStart;
	}

	public void setRangeEnd(T rangeEnd) {
		this.rangeEnd = rangeEnd;
	}

	public void setIncludeRangeEnd(boolean includeRangeEnd) {
		this.includeRangeEnd = includeRangeEnd;
	}

	public void setMaxFacetCount(int maxFacetCount) {
		this.maxFacetCount = maxFacetCount;
	}

	public void makeRange() {
		Class<?> type = getRangeType();
		assertValidRangeType( type );
		FacetRange<T> facetRange = new FacetRange<T>(
				type,
				Range.between( rangeStart, includeRangeStart ? RangeBoundInclusion.INCLUDED : RangeBoundInclusion.EXCLUDED,
						rangeEnd, includeRangeEnd ? RangeBoundInclusion.INCLUDED : RangeBoundInclusion.EXCLUDED ),
				fieldName
		);
		rangeList.add( facetRange );
		rangeStart = null;
		rangeEnd = null;
		includeRangeStart = true;
		includeRangeEnd = true;
	}

	private void assertValidRangeType(Class<?> clazz) {
		if ( !allowedRangeTypes.contains( clazz.getName() ) ) {
			throw log.unsupportedParameterTypeForRangeFaceting(
					clazz.getName(),
					StringHelper.join( allowedRangeTypes, "," )
			);
		}
	}

	private Class<?> getRangeType() {
		if ( rangeStart == null && rangeEnd == null ) {
			throw log.noStartOrEndSpecifiedForRangeQuery( name );
		}
		T tmp = rangeStart;
		if ( tmp == null ) {
			tmp = rangeEnd;
		}
		return tmp.getClass();
	}

	FacetingRequest getFacetingRequest() {
		FacetingRequestImpl request;
		if ( isRangeQuery ) {
			request = new RangeFacetRequest<>( name, fieldName, rangeList );
		}
		else {
			if ( FacetSortOrder.RANGE_DEFINITION_ORDER.equals( sort ) ) {
				throw log.rangeDefinitionOrderRequestedForDiscreteFacetRequest();
			}
			request = new DiscreteFacetRequest( name, fieldName );
		}
		request.setSort( sort );
		request.setIncludeZeroCounts( includeZeroCount );
		request.setMaxNumberOfFacets( maxFacetCount );
		return request;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "FacetBuildingContext" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( ", fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( ", sort=" ).append( sort );
		sb.append( ", includeZeroCount=" ).append( includeZeroCount );
		sb.append( ", isRangeQuery=" ).append( isRangeQuery );
		sb.append( ", rangeList=" ).append( rangeList );
		sb.append( ", rangeStart=" ).append( rangeStart );
		sb.append( ", includeRangeStart=" ).append( includeRangeStart );
		sb.append( ", rangeEnd=" ).append( rangeEnd );
		sb.append( ", includeRangeEnd=" ).append( includeRangeEnd );
		sb.append( '}' );
		return sb.toString();
	}
}

