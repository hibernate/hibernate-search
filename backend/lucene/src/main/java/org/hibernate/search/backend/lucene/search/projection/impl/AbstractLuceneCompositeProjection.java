/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Arrays;
import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

abstract class AbstractLuceneCompositeProjection<P>
		implements LuceneSearchProjection<Object[], P> {

	private final Set<String> indexNames;

	private final LuceneSearchProjection<?, ?>[] children;

	AbstractLuceneCompositeProjection(Set<String> indexNames,
			LuceneSearchProjection<?, ?> ... children) {
		this.indexNames = indexNames;
		this.children = children;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "children=" + Arrays.toString( children )
				+ "]";
	}

	@Override
	public final void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		for ( LuceneSearchProjection<?, ?> child : children ) {
			child.contributeCollectors( luceneCollectorBuilder );
		}
	}

	@Override
	public final void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		for ( LuceneSearchProjection<?, ?> child : children ) {
			child.contributeFields( builder );
		}
	}

	@Override
	public final Object[] extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		Object[] extractedData = new Object[children.length];

		for ( int i = 0; i < extractedData.length; i++ ) {
			LuceneSearchProjection<?, ?> child = children[i];
			extractedData[i] = child.extract(
					mapper, documentResult, context
			);
		}

		return extractedData;
	}

	@Override
	public final P transform(LoadingResult<?> loadingResult, Object[] extractedData,
			SearchProjectionTransformContext context) {
		// Transform in-place
		for ( int i = 0; i < extractedData.length; i++ ) {
			LuceneSearchProjection<?, ?> child = children[i];
			Object extractedElement = extractedData[i];
			extractedData[i] = LuceneSearchProjection.transformUnsafe(
					child, loadingResult, extractedElement, context
			);
		}

		return doTransform( extractedData );
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	/**
	 * @param childResults An object array guaranteed to contain
	 * the result of calling {@link LuceneSearchProjection#extract(ProjectionHitMapper, LuceneResult, SearchProjectionExtractContext)},
	 * then {@link LuceneSearchProjection#transform(LoadingResult, Object, SearchProjectionTransformContext)},
	 * for each child projection.
	 * Each result has the same index as the child projection it originated from.
	 * @return The combination of the child results to return from {@link #transform(LoadingResult, Object[], SearchProjectionTransformContext)}.
	 */
	abstract P doTransform(Object[] childResults);
}
