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
	 * A user specified name for the facet request
	 */
	private final String name;

	/**
	 * The document field name to facet on
	 */
	private final String fieldName;

	/**
	 * Specified in which order the facets will be returned
	 */
	private FacetSortOrder sort = FacetSortOrder.COUNT_DESC;

	/**
	 * Whether a facet value with 0 occurrences
	 */
	private boolean includeZeroCounts = true;

	public FacetRequest(String name, String fieldName) {
		if ( name == null ) {
			throw new IllegalArgumentException( "The request name name cannot be null" );
		}
		if ( fieldName == null ) {
			throw new IllegalArgumentException( "The field name cannot be null" );
		}
		this.name = name;
		this.fieldName = fieldName;
	}

	public String getName() {
		return name;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setSort(FacetSortOrder sort) {
		this.sort = sort;
	}

	public FacetSortOrder getSort() {
		return sort;
	}

	public abstract Class<?> getFieldCacheType();

	public boolean includeZeroCounts() {
		return includeZeroCounts;
	}

	public void setIncludeZeroCounts(boolean includeZeroCounts) {
		this.includeZeroCounts = includeZeroCounts;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer();
		sb.append( "FacetRequest" );
		sb.append( "{name='" ).append( name ).append( '\'' );
		sb.append( ", fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( ", sort=" ).append( sort );
		sb.append( ", includeZeroCounts=" ).append( includeZeroCounts );
		sb.append( '}' );
		return sb.toString();
	}
}


