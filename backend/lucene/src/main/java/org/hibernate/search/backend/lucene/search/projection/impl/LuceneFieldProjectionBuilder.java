/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneFieldProjectionBuilder<F, V> implements FieldProjectionBuilder<V> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneSearchContext searchContext;
	private final LuceneSearchFieldContext<F> field;

	private final ProjectionConverter<? super F, V> converter;
	private final LuceneFieldCodec<F> codec;

	public LuceneFieldProjectionBuilder(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field,
			ProjectionConverter<? super F, V> converter, LuceneFieldCodec<F> codec) {
		this.searchContext = searchContext;
		this.field = field;
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
		if ( accumulatorProvider.isSingleValued() && field.multiValuedInRoot() ) {
			throw log.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(), field.eventContext() );
		}
		return new LuceneFieldProjection<>( searchContext.indexes().indexNames(), field.absolutePath(),
				field.nestedDocumentPath(), codec, converter, accumulatorProvider.get() );
	}
}
