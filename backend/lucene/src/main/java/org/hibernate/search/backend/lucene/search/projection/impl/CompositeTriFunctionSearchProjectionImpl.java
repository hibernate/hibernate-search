/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.util.function.TriFunction;

public class CompositeTriFunctionSearchProjectionImpl<P1, P2, P3, T> implements CompositeSearchProjection<T> {

	private final TriFunction<P1, P2, P3, T> transformer;

	private final LuceneSearchProjection<P1> projection1;

	private final LuceneSearchProjection<P2> projection2;

	private final LuceneSearchProjection<P3> projection3;

	public CompositeTriFunctionSearchProjectionImpl(TriFunction<P1, P2, P3, T> transformer,
			LuceneSearchProjection<P1> projection1, LuceneSearchProjection<P2> projection2,
			LuceneSearchProjection<P3> projection3) {
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
	public void contributeFields(Set<String> absoluteFieldPaths) {
		projection1.contributeFields( absoluteFieldPaths );
		projection2.contributeFields( absoluteFieldPaths );
		projection3.contributeFields( absoluteFieldPaths );
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> projectionHitMapper, LuceneResult luceneResult,
			SearchProjectionExecutionContext context) {
		return new Object[] {
				projection1.extract( projectionHitMapper, luceneResult, context ),
				projection2.extract( projectionHitMapper, luceneResult, context ),
				projection3.extract( projectionHitMapper, luceneResult, context )
		};
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData) {
		Object[] extractedElements = (Object[]) extractedData;

		return transformer.apply(
				projection1.transform( loadingResult, extractedElements[0] ),
				projection2.transform( loadingResult, extractedElements[1] ),
				projection3.transform( loadingResult, extractedElements[2] )
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
