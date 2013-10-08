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

import org.apache.lucene.search.Query;

/**
 * A single facet (field value and count).
 *
 * @author Hardy Ferentschik
 */
public interface Facet {
	/**
	 * @return the faceting name this {@code Facet}	belongs to.
	 *
	 * @see org.hibernate.search.query.facet.FacetingRequest#getFacetingName()
	 */
	String getFacetingName();

	/**
	 * Return the {@code Document} field name this facet is targeting.
	 * The field needs to be indexed with {@code Analyze.NO}.
	 *
	 * @return the {@code Document} field name this facet is targeting.
	 */
	String getFieldName();

	/**
	 * @return the value of this facet. In case of a discrete facet it is the actual
	 *         {@code Document} field value. In case of a range query the value is a
	 *         string representation of the range.
	 */
	String getValue();

	/**
	 * @return the facet count.
	 */
	int getCount();

	/**
	 * @return a Lucene {@link Query} which which can be executed to retrieve all
	 *         documents matching the value of this facet.
	 */
	Query getFacetQuery();
}


