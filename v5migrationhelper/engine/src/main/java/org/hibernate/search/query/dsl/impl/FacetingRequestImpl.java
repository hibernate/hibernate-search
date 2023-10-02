/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import java.util.List;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.query.facet.Facet;
import org.hibernate.search.query.facet.FacetSortOrder;
import org.hibernate.search.query.facet.FacetingRequest;

/**
 * Base class for faceting requests.
 *
 * @author Hardy Ferentschik
 * @param <A> The type of aggregations
 */
public abstract class FacetingRequestImpl<A> implements FacetingRequest {
	/**
	 * A user specified key for the facet request
	 */
	private final AggregationKey<A> key;

	/**
	 * The document facet name to facet on (@Facet.name)
	 */
	private final String fieldName;

	/**
	 * Specified in which order the facets will be returned
	 */
	protected FacetSortOrder sort = FacetSortOrder.COUNT_DESC;

	/**
	 * Whether a facet value with 0 occurrences
	 */
	protected boolean includeZeroCounts = false;

	/**
	 * The maximum number of {@link Facet}s to return for this request. A negative value means that all
	 * facets will be included
	 */
	protected int maxNumberOfFacets = 1;

	public FacetingRequestImpl(String name, String fieldName) {
		if ( name == null ) {
			throw new IllegalArgumentException( "The request name name cannot be null" );
		}
		if ( fieldName == null ) {
			throw new IllegalArgumentException( "The field name cannot be null" );
		}
		this.key = AggregationKey.of( name );
		this.fieldName = fieldName;
	}

	@Override
	public String getFacetingName() {
		return key.name();
	}

	public AggregationKey<A> getKey() {
		return key;
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

	public abstract AggregationFinalStep<A> requestAggregation(SearchAggregationFactory factory);

	public abstract List<Facet> toFacets(A aggregation);

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
		sb.append( "{name='" ).append( key.name() ).append( '\'' );
		sb.append( ", fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( ", sort=" ).append( sort );
		sb.append( ", includeZeroCounts=" ).append( includeZeroCounts );
		sb.append( ", maxNumberOfFacets=" ).append( maxNumberOfFacets );
		sb.append( '}' );
		return sb.toString();
	}
}

