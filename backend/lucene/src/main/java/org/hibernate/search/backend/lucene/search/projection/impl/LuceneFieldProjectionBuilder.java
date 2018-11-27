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
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;

public class LuceneFieldProjectionBuilder<F, T> implements FieldProjectionBuilder<T> {

	private final String absoluteFieldPath;

	private final LuceneFieldCodec<F> codec;

	private final LuceneFieldConverter<F, ?> converter;

	public LuceneFieldProjectionBuilder(String absoluteFieldPath, LuceneFieldCodec<F> codec,
			LuceneFieldConverter<F, ?> converter) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
		this.converter = converter;
	}

	@Override
	public SearchProjection<T> build() {
		return new LuceneFieldProjection<>( absoluteFieldPath, codec, converter );
	}
}
