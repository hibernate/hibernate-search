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

/**
 * Base class for faceting requests.
 *
 * @author Hardy Ferentschik
 */
public abstract class FacetRequest {
	/**
	 * The document field name to facet on
	 */
	final private String fieldName;

	/**
	 * Specified in which order the facets will be returned
	 */
	private FacetSortOrder sort;

	/**
	 * Whether a facet value with 0 occurrences
	 */
	private final boolean includeZeroCounts;

	public FacetRequest(String fieldName) {
		this( fieldName, FacetSortOrder.COUNT_DESC );
	}

	public FacetRequest(String fieldName, FacetSortOrder sort) {
		this( fieldName, sort, true );
	}

	public FacetRequest(String fieldName, FacetSortOrder sort, boolean includeZeroCounts) {
		if ( fieldName == null ) {
			throw new IllegalArgumentException( "The field name cannot be null" );
		}
		this.fieldName = fieldName;
		this.sort = sort;
		this.includeZeroCounts = includeZeroCounts;
	}

	public String getFieldName() {
		return fieldName;
	}

	public boolean includeZeroCounts() {
		return includeZeroCounts;
	}

	public FacetSortOrder getSort() {
		return sort;
	}

	public abstract Class<?> getFieldCacheType();

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "FacetRequest" );
		sb.append( "{fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( ", sort=" ).append( sort );
		sb.append( ", includeZeroCounts=" ).append( includeZeroCounts );
		sb.append( '}' );
		return sb.toString();
	}
}


