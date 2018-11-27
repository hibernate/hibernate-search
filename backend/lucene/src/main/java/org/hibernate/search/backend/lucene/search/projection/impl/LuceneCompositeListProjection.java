/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import static org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection.transformUnsafe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

public class LuceneCompositeListProjection<T> implements LuceneCompositeProjection<List<Object>, T> {

	private final Function<List<?>, T> transformer;

	private final List<LuceneSearchProjection<?, ?>> children;

	public LuceneCompositeListProjection(Function<List<?>, T> transformer,
			List<LuceneSearchProjection<?, ?>> children) {
		this.transformer = transformer;
		this.children = children;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		for ( LuceneSearchProjection<?, ?> child : children ) {
			child.contributeCollectors( luceneCollectorBuilder );
		}
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		for ( LuceneSearchProjection<?, ?> child : children ) {
			child.contributeFields( absoluteFieldPaths );
		}
	}

	@Override
	public List<Object> extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExecutionContext context) {
		List<Object> extractedData = new ArrayList<>( children.size() );

		for ( LuceneSearchProjection<?, ?> child : children ) {
			extractedData.add( child.extract( mapper, documentResult, context ) );
		}

		return extractedData;
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, List<Object> extractedData) {
		for ( int i = 0; i < extractedData.size(); i++ ) {
			extractedData.set( i, transformUnsafe( children.get( i ), loadingResult, extractedData.get( i ) ) );
		}

		return transformer.apply( extractedData );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "children=" ).append( children )
				.append( "]" );
		return sb.toString();
	}
}
