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
 * Faceting request interface.
 *
 * @author Hardy Ferentschik
 */
public interface FacetingRequest {
	/**
	 * @return the name of this faceting request. The faceting name can be an arbitrary string.
	 */
	public String getFacetingName();

	/**
	 * @return the {@code Document} field name on which this faceting request is defined on
	 */
	public String getFieldName();

	/**
	 * @return the sort order of the returned {@code Facet}s for this request
	 */
	public FacetSortOrder getSort();

	/**
	 * @return the maximum number of facets returned for this request
	 */
	public int getMaxNumberOfFacets();

	/**
	 * @return {@code true} if facets with a count of 0 should be included in the returned facet list
	 */
	public boolean hasZeroCountsIncluded();
}


