/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.facet.Facet;

/**
 * A single facet (field value and count).
 *
 * @author Hardy Ferentschik
 */
public abstract class AbstractFacet implements Facet {
	private final String facetingName;
	private final String absoluteFieldPath;
	private final String value;
	private final int count;

	public AbstractFacet(String facetingName, String absoluteFieldPath, String value, int count) {
		this.facetingName = facetingName;
		this.absoluteFieldPath = absoluteFieldPath;
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
		return absoluteFieldPath;
	}

	@Override
	public String getFacetingName() {
		return facetingName;
	}

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
		if ( absoluteFieldPath != null
				? !absoluteFieldPath.equals( that.absoluteFieldPath )
				: that.absoluteFieldPath != null ) {
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
		result = 31 * result + ( absoluteFieldPath != null ? absoluteFieldPath.hashCode() : 0 );
		result = 31 * result + ( value != null ? value.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AbstractFacet" );
		sb.append( "{facetingName='" ).append( facetingName ).append( '\'' );
		sb.append( ", absoluteFieldPath='" ).append( absoluteFieldPath ).append( '\'' );
		sb.append( ", value='" ).append( value ).append( '\'' );
		sb.append( ", count=" ).append( count );
		sb.append( '}' );
		return sb.toString();
	}
}

