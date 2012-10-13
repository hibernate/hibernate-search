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

package org.hibernate.search.query.dsl;

import org.hibernate.search.query.facet.FacetSortOrder;

/**
 * @author Hardy Ferentschik
 */
public interface FacetParameterContext extends FacetTermination {
	/**
	 * @param sort the sort order for the returned facets.
	 *
	 * @return a {@code FacetParameterContext} to continue building the facet request
	 */
	FacetParameterContext orderedBy(FacetSortOrder sort);

	/**
	 * @param zeroCounts Determines whether values with zero counts are included into the facet result
	 *
	 * @return a {@code FacetParameterContext} to continue building the facet request
	 */
	FacetParameterContext includeZeroCounts(boolean zeroCounts);

	/**
	 * Limits the maximum numbers of facets to the specified number.
	 *
	 * @param maxFacetCount the maximum number of facets to include in the response. A negative value means that
	 * all facets will be included
	 * @return a {@code FacetParameterContext} to continue building the facet request
	 */
	FacetParameterContext maxFacetCount(int maxFacetCount);
}


