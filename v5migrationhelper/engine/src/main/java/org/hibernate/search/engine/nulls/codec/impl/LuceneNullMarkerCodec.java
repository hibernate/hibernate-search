/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.nulls.codec.impl;

import org.hibernate.search.bridge.spi.NullMarker;


/**
 * @author Yoann Rodiere
 */
abstract class LuceneNullMarkerCodec implements NullMarkerCodec {

	protected final NullMarker nullMarker;

	public LuceneNullMarkerCodec(NullMarker nullMarker) {
		super();
		this.nullMarker = nullMarker;
	}

	@Override
	public NullMarker getNullMarker() {
		return nullMarker;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + nullMarker + "]";
	}

}
