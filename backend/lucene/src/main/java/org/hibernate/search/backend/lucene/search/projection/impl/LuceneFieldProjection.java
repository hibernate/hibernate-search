/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

class LuceneFieldProjection<F, V> implements LuceneSearchProjection<F, V> {

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String nestedDocumentPath;

	private final LuceneFieldCodec<F> codec;

	private final ProjectionConverter<? super F, V> converter;

	LuceneFieldProjection(Set<String> indexNames, String absoluteFieldPath, String nestedDocumentPath,
			LuceneFieldCodec<F> codec, ProjectionConverter<? super F, V> converter) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedDocumentPath = nestedDocumentPath;
		this.codec = codec;
		this.converter = converter;
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		context.requireTopDocsCollector();
		codec.contributeStoredFields( absoluteFieldPath, context::requireStoredField );
		context.requireNestedDocumentExtraction( nestedDocumentPath );
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
	public Set<String> getIndexNames() {
		return indexNames;
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
