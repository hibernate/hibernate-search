/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;


public class ElasticsearchFieldProjectionBuilder<F, V> implements FieldProjectionBuilder<V> {

	private final String absoluteFieldPath;

	private final FromDocumentFieldValueConverter<? super F, V> converter;
	private final ElasticsearchFieldCodec<F> codec;

	public ElasticsearchFieldProjectionBuilder(String absoluteFieldPath,
			FromDocumentFieldValueConverter<? super F, V> converter,
			ElasticsearchFieldCodec<F> codec) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	public SearchProjection<V> build() {
		return new ElasticsearchFieldProjection<>( absoluteFieldPath, converter, codec );
	}
}
