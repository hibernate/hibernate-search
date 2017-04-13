/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.engine.metadata.impl.FacetMetadata;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * Base class for faceting requests.
 *
 * @author Hardy Ferentschik
 */
public abstract class FacetingRequestImpl implements FacetingRequest {
	/**
	 * A user specified name for the facet request
	 */
	private final String name;

	/**
	 * The document facet name to facet on (@Facet.name)
	 */
	private final String fieldName;

	/**
	 * Specified in which order the facets will be returned
	 */
	private FacetSortOrder sort = FacetSortOrder.COUNT_DESC;

	/**
	 * Whether a facet value with 0 occurrences
	 */
	private boolean includeZeroCounts = false;

	/**
	 * The maximum number of {@link org.hibernate.search.query.facet.Facet}s to return for this request. A negative value means that all
	 * facets will be included
	 */
	private int maxNumberOfFacets = 1;

	public FacetingRequestImpl(String name, String fieldName) {
		if ( name == null ) {
			throw new IllegalArgumentException( "The request name name cannot be null" );
		}
		if ( fieldName == null ) {
			throw new IllegalArgumentException( "The field name cannot be null" );
		}
		this.name = name;
		this.fieldName = fieldName;
	}

	@Override
	public String getFacetingName() {
		return name;
	}

	@Override
	public String getFieldName() {
		return fieldName;
	}

	public void setSort(FacetSortOrder sort) {
		this.sort = sort;
	}

	@Override
	public FacetSortOrder getSort() {
		return sort;
	}

	@Override
	public int getMaxNumberOfFacets() {
		return maxNumberOfFacets;
	}

	public void setMaxNumberOfFacets(int maxNumberOfFacets) {
		this.maxNumberOfFacets = maxNumberOfFacets;
	}

	/**
	 * @return the field value type on which the facets applies. This determines which type of facet query one needs to build.
	 */
	public abstract Class<?> getFacetValueType();

	public abstract Facet createFacet(FacetMetadata facetMetadata, String value, int count);

	@Override
	public boolean hasZeroCountsIncluded() {
		return includeZeroCounts;
	}

	public void setIncludeZeroCounts(boolean includeZeroCounts) {
		this.includeZeroCounts = includeZeroCounts;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "FacetingRequest" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( ", fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( ", sort=" ).append( sort );
		sb.append( ", includeZeroCounts=" ).append( includeZeroCounts );
		sb.append( ", maxNumberOfFacets=" ).append( maxNumberOfFacets );
		sb.append( '}' );
		return sb.toString();
	}
}


