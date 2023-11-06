/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.apache.lucene.codecs.KnnVectorsFormat;

/**
 * Vector field specific codec that allows redefining {@link KnnVectorsFormat}.
 *
 * @param <F> The field type exposed to the mapper.
 */
public interface LuceneVectorFieldCodec<F> extends LuceneStandardFieldCodec<F, byte[]> {

	/**
	 * Custom {@link KnnVectorsFormat knn vector format} that will be used in {@link org.apache.lucene.codecs.perfield.PerFieldKnnVectorsFormat}
	 * and can for example define custom {@code beamWidth} or {@code maxConnections} or even provide a completely custom implementation (needs to be registered via ServiceLoader mechanism).
	 */
	KnnVectorsFormat knnVectorFormat();
}
