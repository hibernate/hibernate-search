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

import org.apache.lucene.search.Query;

import org.hibernate.search.query.facet.Facet;

/**
 * A single facet (field value and count).
 *
 * @author Hardy Ferentschik
 */
public abstract class AbstractFacet implements Facet {
	private final String facetingName;
	private final String fieldName;
	private final String value;
	private final int count;

	public AbstractFacet(String facetingName, String fieldName, String value, int count) {
		this.facetingName = facetingName;
		this.fieldName = fieldName;
		this.count = count;
		this.value = value;
	}

	public int getCount() {
		return count;
	}

	public String getValue() {
		return value;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFacetingName() {
		return facetingName;
	}

	public abstract Query getFacetQuery();

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		AbstractFacet that = (AbstractFacet) o;

		if ( count != that.count ) {
			return false;
		}
		if ( facetingName != null ? !facetingName.equals( that.facetingName ) : that.facetingName != null ) {
			return false;
		}
		if ( fieldName != null ? !fieldName.equals( that.fieldName ) : that.fieldName != null ) {
			return false;
		}
		if ( value != null ? !value.equals( that.value ) : that.value != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = facetingName != null ? facetingName.hashCode() : 0;
		result = 31 * result + ( fieldName != null ? fieldName.hashCode() : 0 );
		result = 31 * result + ( value != null ? value.hashCode() : 0 );
		result = 31 * result + count;
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AbstractFacet" );
		sb.append( "{facetingName='" ).append( facetingName ).append( '\'' );
		sb.append( ", fieldName='" ).append( fieldName ).append( '\'' );
		sb.append( ", value='" ).append( value ).append( '\'' );
		sb.append( ", count=" ).append( count );
		sb.append( '}' );
		return sb.toString();
	}
}


