/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import static org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection.transformUnsafe;

import java.util.Set;
import java.util.function.BiFunction;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

public class LuceneCompositeBiFunctionProjection<P1, P2, T> implements LuceneCompositeProjection<Object[], T> {

	private final BiFunction<P1, P2, T> transformer;

	private final LuceneSearchProjection<?, P1> projection1;

	private final LuceneSearchProjection<?, P2> projection2;

	public LuceneCompositeBiFunctionProjection(BiFunction<P1, P2, T> transformer,
			LuceneSearchProjection<?, P1> projection1, LuceneSearchProjection<?, P2> projection2) {
		this.transformer = transformer;
		this.projection1 = projection1;
		this.projection2 = projection2;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		projection1.contributeCollectors( luceneCollectorBuilder );
		projection2.contributeCollectors( luceneCollectorBuilder );
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		projection1.contributeFields( absoluteFieldPaths );
		projection2.contributeFields( absoluteFieldPaths );
	}

	@Override
	public Object[] extract(ProjectionHitMapper<?, ?> projectionHitMapper, LuceneResult luceneResult,
			SearchProjectionExecutionContext context) {
		return new Object[] {
				projection1.extract( projectionHitMapper, luceneResult, context ),
				projection2.extract( projectionHitMapper, luceneResult, context )
		};
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object[] extractedData) {
		return transformer.apply(
				transformUnsafe( projection1, loadingResult, extractedData[0] ),
				transformUnsafe( projection2, loadingResult, extractedData[1] )
		);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "projection1=" ).append( projection1 )
				.append( ", projection2=" ).append( projection2 )
				.append( "]" );
		return sb.toString();
	}
}
