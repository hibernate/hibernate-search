/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

public class CompositeFunctionSearchProjectionImpl<E, P, T> implements CompositeSearchProjection<E, T> {

	private final Function<P, T> transformer;

	private final LuceneSearchProjection<E, P> projection;

	public CompositeFunctionSearchProjectionImpl(Function<P, T> transformer,
			LuceneSearchProjection<E, P> projection) {
		this.transformer = transformer;
		this.projection = projection;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		projection.contributeCollectors( luceneCollectorBuilder );
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		projection.contributeFields( absoluteFieldPaths );
	}

	@Override
	public E extract(ProjectionHitMapper<?, ?> projectionHitMapper, LuceneResult luceneResult,
			SearchProjectionExecutionContext context) {
		return projection.extract( projectionHitMapper, luceneResult, context );
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, E extractedData) {
		return transformer.apply( projection.transform( loadingResult, extractedData ) );
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
