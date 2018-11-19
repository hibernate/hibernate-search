/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

public class CompositeListSearchProjectionImpl<T> implements CompositeSearchProjection<T> {

	private final Function<List<?>, T> transformer;

	private final List<LuceneSearchProjection<?>> children;

	public CompositeListSearchProjectionImpl(Function<List<?>, T> transformer,
			List<LuceneSearchProjection<?>> children) {
		this.transformer = transformer;
		this.children = children;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		for ( LuceneSearchProjection<?> child : children ) {
			child.contributeCollectors( luceneCollectorBuilder );
		}
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		for ( LuceneSearchProjection<?> child : children ) {
			child.contributeFields( absoluteFieldPaths );
		}
	}

	@Override
	public Object extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExecutionContext context) {
		List<Object> extractedData = new ArrayList<>( children.size() );

		for ( LuceneSearchProjection<?> child : children ) {
			extractedData.add( child.extract( mapper, documentResult, context ) );
		}

		return extractedData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T transform(LoadingResult<?> loadingResult, Object extractedData) {
		List<Object> extractedElements = (List<Object>) extractedData;

		for ( int i = 0; i < extractedElements.size(); i++ ) {
			extractedElements.set( i, children.get( i ).transform( loadingResult, extractedElements.get( i ) ) );
		}

		return transformer.apply( extractedElements );
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
