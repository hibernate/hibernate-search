/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneFieldConverter;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldSearchProjectionBuilder;

public class FieldSearchProjectionBuilderImpl<T> implements FieldSearchProjectionBuilder<T> {

	private final String absoluteFieldPath;

	private final LuceneFieldCodec<T> codec;

	private final LuceneFieldConverter<T, ?> converter;

	public FieldSearchProjectionBuilderImpl(String absoluteFieldPath, LuceneFieldCodec<T> codec,
			LuceneFieldConverter<T, ?> converter) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
		this.converter = converter;
	}

	@Override
	public SearchProjection<T> build() {
		return new FieldSearchProjectionImpl<>( absoluteFieldPath, codec, converter );
	}
}
