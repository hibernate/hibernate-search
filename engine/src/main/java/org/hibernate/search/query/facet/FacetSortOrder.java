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
 * Specifies the order in which the facets are returned.
 *
 * @author Hardy Ferentschik
 */
public enum FacetSortOrder {
	/**
	 * Facets are returned by count with the lowest count first
	 */
	COUNT_ASC,

	/**
	 * Facets are returned by count with the lowest count first
	 */
	COUNT_DESC,

	/**
	 * Facets are returned in the alphabetical order
	 */
	FIELD_VALUE,

	/**
	 * The order in which ranges were defined. Only valid for range faceting
	 * @deprecated use {@link #RANGE_DEFINITION_ORDER}
	 */
	@Deprecated
	RANGE_DEFINITION_ODER,

	/**
	 * The order in which ranges were defined. Only valid for range faceting
	 */
	RANGE_DEFINITION_ORDER
}
