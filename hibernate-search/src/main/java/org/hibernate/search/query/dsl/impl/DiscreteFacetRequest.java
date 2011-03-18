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

package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import org.hibernate.search.query.facet.Facet;

/**
 * @author Hardy Ferentschik
 */
public class DiscreteFacetRequest extends FacetingRequestImpl {
	DiscreteFacetRequest(String name, String fieldName) {
		super( name, fieldName );
	}

	@Override
	public Class<?> getFieldCacheType() {
		return String.class;
	}

	@Override
	public Facet createFacet(String value, int count) {
		return new SimpleFacet( getFacetingName(), getFieldName(), value, count );
	}

	static class SimpleFacet extends AbstractFacet {
		SimpleFacet(String facetingName, String fieldName, String value, int count) {
			super( facetingName, fieldName, value, count );
		}

		@Override
		public Query getFacetQuery() {
			return new TermQuery( new Term( getFieldName(), getValue() ) );
		}
	}
}