/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.reporting.impl.LuceneSearchHints;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.DocumentReferenceValues;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

public class LuceneEntityReferenceProjection<R> extends AbstractLuceneProjection<R>
		implements LuceneSearchProjection.Extractor<DocumentReference, R> {

	LuceneEntityReferenceProjection(LuceneSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, R> request(ProjectionRequestContext context) {
		context.checkNotNested(
				ProjectionTypeKeys.ENTITY_REFERENCE,
				LuceneSearchHints.INSTANCE.entityReferenceProjectionNestingNotSupportedHint()
		);
		return this;
	}

	@Override
	public Values<DocumentReference> values(ProjectionExtractContext context) {
		return DocumentReferenceValues.simple( context.collectorExecutionContext() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public R transform(LoadingResult<?> loadingResult, DocumentReference extractedData,
			ProjectionTransformContext context) {
		return (R) loadingResult.convertReference( extractedData );
	}
}
