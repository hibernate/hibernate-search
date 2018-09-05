/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.nulls.codec.impl;

import org.hibernate.search.bridge.spi.NullMarker;
import org.hibernate.search.engine.nulls.codec.impl.NullMarkerCodec;

/**
 * A base class for {@link NullMarkerCodec}s that index null values as a non-null token.
 * <p>
 * This is necessary because Elasticsearch doesn't support 'null_value' on the 'text' datatype.
 *
 * @author Yoann Rodiere
 */
abstract class ElasticsearchAsTokenNullMarkerCodec implements NullMarkerCodec {

	protected final NullMarker nullMarker;

	public ElasticsearchAsTokenNullMarkerCodec(NullMarker nullMarker) {
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