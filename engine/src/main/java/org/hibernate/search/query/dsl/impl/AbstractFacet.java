/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	@Override
	public int getCount() {
		return count;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public String getFacetingName() {
		return facetingName;
	}

	@Override
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


