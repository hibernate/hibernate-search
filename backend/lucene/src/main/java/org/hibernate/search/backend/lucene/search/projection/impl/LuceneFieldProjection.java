/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

class LuceneFieldProjection<F, V> implements LuceneSearchProjection<F, V> {

	private final String absoluteFieldPath;

	private final LuceneFieldCodec<F> codec;

	private final FromDocumentFieldValueConverter<? super F, V> converter;

	LuceneFieldProjection(String absoluteFieldPath, LuceneFieldCodec<F> codec,
			FromDocumentFieldValueConverter<? super F, V> converter) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
		this.converter = converter;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	@Override
	public void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		codec.contributeStoredFields( absoluteFieldPath, builder::add );
	}

	@Override
	public F extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		return codec.decode( documentResult.getDocument(), absoluteFieldPath );
	}

	@Override
	public V transform(LoadingResult<?> loadingResult, F extractedData,
			SearchProjectionTransformContext context) {
		FromDocumentFieldValueConvertContext convertContext = context.getFromDocumentFieldValueConvertContext();
		return converter.convert( extractedData, convertContext );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( absoluteFieldPath )
				.append( "]" );
		return sb.toString();
	}
}
