/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;


import static org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection.transformUnsafe;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.common.function.TriFunction;

public class LuceneCompositeTriFunctionProjection<P1, P2, P3, P> implements LuceneCompositeProjection<Object[], P> {

	private final TriFunction<P1, P2, P3, P> transformer;

	private final LuceneSearchProjection<?, P1> projection1;

	private final LuceneSearchProjection<?, P2> projection2;

	private final LuceneSearchProjection<?, P3> projection3;

	public LuceneCompositeTriFunctionProjection(TriFunction<P1, P2, P3, P> transformer,
			LuceneSearchProjection<?, P1> projection1, LuceneSearchProjection<?, P2> projection2,
			LuceneSearchProjection<?, P3> projection3) {
		this.transformer = transformer;
		this.projection1 = projection1;
		this.projection2 = projection2;
		this.projection3 = projection3;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		projection1.contributeCollectors( luceneCollectorBuilder );
		projection2.contributeCollectors( luceneCollectorBuilder );
		projection3.contributeCollectors( luceneCollectorBuilder );
	}

	@Override
	public void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		projection1.contributeFields( builder );
		projection2.contributeFields( builder );
		projection3.contributeFields( builder );
	}

	@Override
	public Object[] extract(ProjectionHitMapper<?, ?> projectionHitMapper, LuceneResult luceneResult,
			SearchProjectionExtractContext context) {
		return new Object[] {
				projection1.extract( projectionHitMapper, luceneResult, context ),
				projection2.extract( projectionHitMapper, luceneResult, context ),
				projection3.extract( projectionHitMapper, luceneResult, context )
		};
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, Object[] extractedData,
			SearchProjectionTransformContext context) {
		return transformer.apply(
				transformUnsafe( projection1, loadingResult, extractedData[0], context ),
				transformUnsafe( projection2, loadingResult, extractedData[1], context ),
				transformUnsafe( projection3, loadingResult, extractedData[2], context )
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "projection1=" ).append( projection1 )
				.append( ", projection2=" ).append( projection2 )
				.append( ", projection3=" ).append( projection3 )
				.append( "]" );
		return sb.toString();
	}
}
