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

import java.util.List;

/**
 * @author Hardy Ferentschik
 */
public class FacetResult {
	private final String name;
	private final String fieldName;
	private final List<Facet> facets;

	public FacetResult(String name, String fieldName, List<Facet> facets) {
		this.name = name;
		this.fieldName = fieldName;
		this.facets = facets;
	}

	public List<Facet> getFacets() {
		return facets;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "FacetResult" );
		sb.append( "{facets=" ).append( facets );
		sb.append( '}' );
		return sb.toString();
	}
}


