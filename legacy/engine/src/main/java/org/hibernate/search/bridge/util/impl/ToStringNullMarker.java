/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.bridge.util.impl;

import org.hibernate.search.bridge.spi.NullMarker;

/**
 * A null marker that uses toString() to retrieve the null string representation.
 *
 * @author Yoann Rodiere
 */
public class ToStringNullMarker implements NullMarker {

	private final Object indexNullAs;
	private final String stringRepresentation;

	public ToStringNullMarker(final Object indexNullAs) {
		if ( indexNullAs == null ) {
			throw new NullPointerException( "The constructor parameter is mandatory" );
		}
		this.indexNullAs = indexNullAs;
		this.stringRepresentation = indexNullAs.toString();
	}

	@Override
	public String nullRepresentedAsString() {
		return stringRepresentation;
	}

	@Override
	public Object nullEncoded() {
		return indexNullAs;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + indexNullAs + "]";
	}

}
