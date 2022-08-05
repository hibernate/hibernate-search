/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.DocumentReferenceValues;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneDocumentReference;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class LuceneEntityLoadingProjection<E> extends AbstractLuceneProjection<E>
		implements LuceneSearchProjection.Extractor<Object, E> {

	LuceneEntityLoadingProjection(LuceneSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, E> request(ProjectionRequestContext context) {
		return this;
	}

	@Override
	public Values<Object> values(ProjectionExtractContext context) {
		ProjectionHitMapper<?, ?> mapper = context.projectionHitMapper();
		return new DocumentReferenceValues<Object>( context.collectorExecutionContext() ) {
			@Override
			protected Object toReference(String typeName, String identifier) {
				return mapper.planLoading( new LuceneDocumentReference( typeName, identifier ) );
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	public E transform(LoadingResult<?, ?> loadingResult, Object extractedData,
			ProjectionTransformContext context) {
		E loaded = (E) loadingResult.get( extractedData );
		if ( loaded == null ) {
			context.reportFailedLoad();
		}
		return loaded;
	}
}
