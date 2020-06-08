/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;


public class ElasticsearchFieldProjectionBuilder<F, V> implements FieldProjectionBuilder<V> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String[] absoluteFieldPathComponents;

	private final ProjectionConverter<? super F, V> converter;
	private final ElasticsearchFieldCodec<F> codec;

	public ElasticsearchFieldProjectionBuilder(Set<String> indexNames, String absoluteFieldPath,
			String[] absoluteFieldPathComponents,
			ProjectionConverter<? super F, V> converter,
			ElasticsearchFieldCodec<F> codec) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.absoluteFieldPathComponents = absoluteFieldPathComponents;
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	public <R> SearchProjection<R> build(ProjectionAccumulator.Provider<V, R> accumulatorProvider) {
		return new ElasticsearchFieldProjection<>( indexNames, absoluteFieldPath, absoluteFieldPathComponents,
				codec, converter, accumulatorProvider.get() );
	}
}
