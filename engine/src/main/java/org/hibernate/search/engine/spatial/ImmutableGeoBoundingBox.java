/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.spatial;

import java.util.Objects;

import org.hibernate.search.util.common.impl.Contracts;

final class ImmutableGeoBoundingBox implements GeoBoundingBox {

	private final GeoPoint topLeft;

	private final GeoPoint bottomRight;

	ImmutableGeoBoundingBox(GeoPoint topLeft, GeoPoint bottomRight) {
		Contracts.assertNotNull( topLeft, "topLeft" );
		Contracts.assertNotNull( bottomRight, "bottomRight" );

		this.topLeft = topLeft;
		this.bottomRight = bottomRight;
	}

	@Override
	public GeoPoint topLeft() {
		return topLeft;
	}

	@Override
	public GeoPoint bottomRight() {
		return bottomRight;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}

		if ( obj == null || getClass() != obj.getClass() ) {
			return false;
		}

		ImmutableGeoBoundingBox that = (ImmutableGeoBoundingBox) obj;

		return Objects.equals( that.topLeft, topLeft )
				&& Objects.equals( that.bottomRight, bottomRight );
	}

	@Override
	public int hashCode() {
		return Objects.hash( topLeft, bottomRight );
	}

	@Override
	public String toString() {
		return "ImmutableGeoBoundingBox[topLeft=" + topLeft + ", bottomRight=" + bottomRight + "]";
	}
}
