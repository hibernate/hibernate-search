/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.DocumentReferenceCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class LuceneReferenceProjection<R> implements LuceneSearchProjection<R, R> {

	private final Set<String> indexNames;

	public LuceneReferenceProjection(Set<String> indexNames) {
		this.indexNames = indexNames;
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		context.requireCollector( DocumentReferenceCollector.FACTORY );
	}

	@SuppressWarnings("unchecked")
	@Override
	public R extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		DocumentReference documentReference =
				context.getCollector( DocumentReferenceCollector.FACTORY ).get( documentResult.getDocId() );
		return (R) mapper.convertReference( documentReference );
	}

	@Override
	public R transform(LoadingResult<?> loadingResult, R extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
