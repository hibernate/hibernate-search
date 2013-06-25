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

import org.hibernate.search.SearchException;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;

import static org.hibernate.search.util.impl.CollectionHelper.newArrayList;

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
		allowedRangeTypes.add( Integer.class );
		allowedRangeTypes.add( Long.class );
		allowedRangeTypes.add( Double.class );
		allowedRangeTypes.add( Float.class );
		allowedRangeTypes.add( Date.class );
	}

	private final SearchFactoryImplementor factory;
	private final Class<?> entityType;

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
	private DocumentBuilderIndexedEntity<?> documentBuilder;

	public FacetBuildingContext(SearchFactoryImplementor factory, Class<?> entityType) {
		this.factory = factory;
		this.entityType = entityType;
	}

	void setName(String name) {
		this.name = name;
	}

	void setFieldName(String fieldName) {
		this.fieldName = fieldName;
		assertFacetingFieldExists();
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
				rangeStart,
				rangeEnd,
				includeRangeStart,
				includeRangeEnd,
				fieldName,
				documentBuilder
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

	FacetingRequest getFacetingRequest() {
		FacetingRequestImpl request;
		if ( isRangeQuery ) {
			request = new RangeFacetRequest<T>( name, fieldName, rangeList, documentBuilder );
		}
		else {
			if ( FacetSortOrder.RANGE_DEFINITION_ODER.equals( sort )
					|| FacetSortOrder.RANGE_DEFINITION_ORDER.equals( sort ) ) {
				throw new SearchException(
						"RANGE_DEFINITION_ORDER is not a valid sort order for a discrete faceting request."
				);
			}
			request = new DiscreteFacetRequest( name, fieldName );
		}
		request.setSort( sort );
		request.setIncludeZeroCounts( includeZeroCount );
		request.setMaxNumberOfFacets( maxFacetCount );
		return request;
	}

	private void assertFacetingFieldExists() {
		if ( fieldName == null ) {
			throw new IllegalArgumentException( "null is an invalid field name" );
		}

		EntityIndexBinding indexBinding = factory.getIndexBinding( entityType );
		if ( indexBinding == null ) {
			throw new SearchException(
					"Entity " + entityType.getName()
							+ " is not an indexed entity. Unable to create faceting request"
			);
		}
		documentBuilder = indexBinding.getDocumentBuilder();
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


