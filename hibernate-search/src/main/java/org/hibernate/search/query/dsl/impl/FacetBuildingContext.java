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

import org.hibernate.search.SearchException;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;

import static org.hibernate.search.util.CollectionHelper.newArrayList;

/**
 * @author Hardy Ferentschik
 */
class FacetBuildingContext<T> {
	/**
	 * The list of types which are supported for range faceting
	 */
	private static final List<Class<?>> allowedRangeTypes = newArrayList();

	static {
		allowedRangeTypes.add( String.class );
		allowedRangeTypes.add( String.class );
		allowedRangeTypes.add( Integer.class );
		allowedRangeTypes.add( Long.class );
		allowedRangeTypes.add( Double.class );
		allowedRangeTypes.add( Float.class );
	}

	private String name;
	private String fieldName;
	private FacetSortOrder sort = FacetSortOrder.COUNT_DESC;
	private boolean includeZeroCount = true;
	private boolean isRangeQuery = false;
	private List<FacetRange<T>> rangeList = newArrayList();
	private T rangeStart;
	private boolean includeRangeStart = true;
	private T rangeEnd;
	private boolean includeRangeEnd = true;
	private int maxFacetCount = -1;

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
				nullSafeGetMin( rangeStart, type ),
				nullSafeGetMax( rangeEnd, type ),
				includeRangeStart,
				includeRangeEnd
		);
		rangeList.add( facetRange );
		rangeStart = null;
		rangeEnd = null;
		includeRangeStart = true;
		includeRangeEnd = true;
	}

	private void assertValidRangeType(Class<?> clazz) {
		if ( !allowedRangeTypes.contains( clazz ) ) {
			throw new SearchException( "Unsupported range type: " + clazz.getName() );
		}
	}

	private Class<?> getRangeType() {
		if ( rangeStart == null && rangeEnd == null ) {
			throw new SearchException( "You have to at least specify a start or end of the range" );
		}
		T tmp = rangeStart;
		if ( tmp == null ) {
			tmp = rangeEnd;
		}
		return tmp.getClass();
	}

	private T nullSafeGetMin(T min, Class<?> type) {
		if ( min != null ) {
			return min;
		}
		T minValue;
		if ( Double.class.equals( type ) ) {
			minValue = (T) Double.valueOf( Double.MIN_VALUE );
		}
		else if ( Float.class.equals( type ) ) {
			minValue = (T) Float.valueOf( Float.MIN_VALUE );
		}
		else if ( Integer.class.equals( type ) ) {
			minValue = (T) Integer.valueOf( Integer.MIN_VALUE );
		}
		else if ( Long.class.equals( type ) ) {
			minValue = (T) Long.valueOf( Long.MIN_VALUE );
		}
		else {
			throw new SearchException( "Unsupported range type: " + type.getName() );
		}
		return minValue;
	}

	private T nullSafeGetMax(T max, Class<?> type) {
		if ( max != null ) {
			return max;
		}
		T maxValue;
		if ( Double.class.equals( type ) ) {
			maxValue = (T) Double.valueOf( Double.MAX_VALUE );
		}
		else if ( Float.class.equals( type ) ) {
			maxValue = (T) Float.valueOf( Float.MAX_VALUE );
		}
		else if ( Integer.class.equals( type ) ) {
			maxValue = (T) Integer.valueOf( Integer.MAX_VALUE );
		}
		else if ( Long.class.equals( type ) ) {
			maxValue = (T) Long.valueOf( Long.MAX_VALUE );
		}
		else {
			throw new SearchException( "Unsupported range type: " + type.getName() );
		}
		return maxValue;
	}

	FacetingRequest getFacetingRequest() {
		FacetingRequest request;
		if ( isRangeQuery ) {
			request = new RangeFacetRequest<T>( name, fieldName, rangeList );
		}
		else {
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


