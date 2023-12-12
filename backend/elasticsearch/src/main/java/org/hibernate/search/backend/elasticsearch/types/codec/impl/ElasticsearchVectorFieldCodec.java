/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.codec.impl;

public interface ElasticsearchVectorFieldCodec<F> extends ElasticsearchFieldCodec<F> {

	/**
	 * @return The type of vector elements expected to get either {@code float.class} or {@code byte.class}.
	 */
	Class<?> vectorElementsType();

	/**
	 * @return The number of dimensions (array length) of vectors to be indexed that this codec can process.
	 */
	int getConfiguredDimensions();
}
