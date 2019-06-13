/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;

public class LuceneFieldProjectionBuilder<F, V> implements FieldProjectionBuilder<V> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;

	private final FromDocumentFieldValueConverter<? super F, V> converter;
	private final LuceneFieldCodec<F> codec;

	public LuceneFieldProjectionBuilder(Set<String> indexNames, String absoluteFieldPath,
			FromDocumentFieldValueConverter<? super F, V> converter,
			LuceneFieldCodec<F> codec) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	public SearchProjection<V> build() {
		return new LuceneFieldProjection<>( indexNames, absoluteFieldPath, codec, converter );
	}
}
