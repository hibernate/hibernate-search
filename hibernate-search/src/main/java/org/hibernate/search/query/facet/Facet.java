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
	 * @return the faceting name this {@code Facet}	belongs to. See {@link org.hibernate.search.query.facet.FacetingRequest#getFacetingName()}.
	 */
	public String getFacetingName();

	/**
	 * @return the {@code Document} field name this facet was created for
	 */
	public String getFieldName();

	/**
	 * @return the value of this facet. In case of a discrete facet it is the actual {@code Document} field value. In case of
	 *         a range query the value is a string representation of the range
	 */
	public String getValue();

	/**
	 * @return the faceting count
	 */
	public int getCount();

	/**
	 * @return a Lucene {@link Query} which can be applied just targeted all documents matching the value of this facet
	 */
	public Query getFacetQuery();
}


