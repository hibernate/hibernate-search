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
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class LuceneCompositeListProjection<P> implements LuceneCompositeProjection<List<Object>, P> {

	private final Function<List<?>, P> transformer;

	private final List<LuceneSearchProjection<?, ?>> children;

	public LuceneCompositeListProjection(Function<List<?>, P> transformer,
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
	public void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		for ( LuceneSearchProjection<?, ?> child : children ) {
			child.contributeFields( builder );
		}
	}

	@Override
	public List<Object> extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		List<Object> extractedData = new ArrayList<>( children.size() );

		for ( LuceneSearchProjection<?, ?> child : children ) {
			extractedData.add( child.extract( mapper, documentResult, context ) );
		}

		return extractedData;
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, List<Object> extractedData,
			SearchProjectionTransformContext context) {
		for ( int i = 0; i < extractedData.size(); i++ ) {
			extractedData.set( i, transformUnsafe( children.get( i ), loadingResult, extractedData.get( i ), context ) );
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
