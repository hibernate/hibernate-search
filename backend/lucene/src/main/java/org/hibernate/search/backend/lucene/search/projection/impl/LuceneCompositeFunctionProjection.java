/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class LuceneCompositeFunctionProjection<E, P1, P> implements LuceneCompositeProjection<E, P> {

	private final Function<P1, P> transformer;

	private final LuceneSearchProjection<E, P1> projection;

	public LuceneCompositeFunctionProjection(Function<P1, P> transformer,
			LuceneSearchProjection<E, P1> projection) {
		this.transformer = transformer;
		this.projection = projection;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		projection.contributeCollectors( luceneCollectorBuilder );
	}

	@Override
	public void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		projection.contributeFields( builder );
	}

	@Override
	public E extract(ProjectionHitMapper<?, ?> projectionHitMapper, LuceneResult luceneResult,
			SearchProjectionExtractContext context) {
		return projection.extract( projectionHitMapper, luceneResult, context );
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, E extractedData,
			SearchProjectionTransformContext context) {
		return transformer.apply( projection.transform( loadingResult, extractedData, context ) );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "projection=" ).append( projection )
				.append( "]" );
		return sb.toString();
	}
}
