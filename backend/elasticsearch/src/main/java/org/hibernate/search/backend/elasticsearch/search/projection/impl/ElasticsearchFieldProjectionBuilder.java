/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;


public class ElasticsearchFieldProjectionBuilder<F, V> implements FieldProjectionBuilder<V> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchSearchContext searchContext;
	private final ElasticsearchSearchFieldContext<F> field;

	private final ProjectionConverter<? super F, V> converter;
	private final ElasticsearchFieldCodec<F> codec;

	public ElasticsearchFieldProjectionBuilder(ElasticsearchSearchContext searchContext,
			ElasticsearchSearchFieldContext<F> field,
			ProjectionConverter<? super F, V> converter,
			ElasticsearchFieldCodec<F> codec) {
		this.searchContext = searchContext;
		this.field = field;
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	public <R> SearchProjection<R> build(ProjectionAccumulator.Provider<V, R> accumulatorProvider) {
		if ( accumulatorProvider.isSingleValued() && field.multiValuedInRoot() ) {
			throw log.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(), field.eventContext() );
		}
		return new ElasticsearchFieldProjection<>( searchContext.indexes().hibernateSearchIndexNames(),
				field.absolutePath(), field.absolutePathComponents(),
				codec::decode, converter, accumulatorProvider.get() );
	}
}
