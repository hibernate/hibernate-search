/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.FieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneFieldProjectionBuilder<F, V> implements FieldProjectionBuilder<V> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String nestedDocumentPath;
	private final boolean multiValuedFieldInRoot;

	private final ProjectionConverter<? super F, V> converter;
	private final LuceneFieldCodec<F> codec;

	public LuceneFieldProjectionBuilder(Set<String> indexNames, String absoluteFieldPath, String nestedDocumentPath,
			boolean multiValuedFieldInRoot,
			ProjectionConverter<? super F, V> converter,
			LuceneFieldCodec<F> codec) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedDocumentPath = nestedDocumentPath;
		this.multiValuedFieldInRoot = multiValuedFieldInRoot;
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
		if ( accumulatorProvider.isSingleValued() && multiValuedFieldInRoot ) {
			throw log.invalidSingleValuedProjectionOnMultiValuedField( absoluteFieldPath,
					EventContexts.fromIndexNames( indexNames ) );
		}
		return new LuceneFieldProjection<>( indexNames, absoluteFieldPath, nestedDocumentPath,
				codec, converter, accumulatorProvider.get() );
	}
}
