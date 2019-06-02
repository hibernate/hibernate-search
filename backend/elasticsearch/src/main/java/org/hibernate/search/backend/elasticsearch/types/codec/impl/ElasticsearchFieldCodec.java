/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import com.google.gson.JsonElement;

/**
 * Defines how a given value will be encoded as JSON and decoded from JSON.
 * <p>
 * Encodes values received from an {@link org.hibernate.search.engine.backend.document.IndexFieldReference} when indexing,
 * and returns decoded values to the {@link org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection}
 * when projecting in a search query.
 */
public interface ElasticsearchFieldCodec<F> {

	JsonElement encode(F value);

	default JsonElement encodeForMissing(F value) {
		return encode( value );
	}

	F decode(JsonElement element);

	/**
	 * Determine whether another codec is compatible with this one, i.e. whether it will encode/decode the information
	 * to/from the document in a compatible way.
	 *
	 * @param other Another {@link ElasticsearchFieldCodec}, never {@code null}.
	 * @return {@code true} if the given codec is compatible. {@code false} otherwise, or when
	 * in doubt.
	 */
	boolean isCompatibleWith(ElasticsearchFieldCodec<?> other);
}
