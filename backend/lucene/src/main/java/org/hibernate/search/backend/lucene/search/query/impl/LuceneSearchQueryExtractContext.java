/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectors;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionTransformContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentFieldValueConvertContextImpl;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

/**
 * The context holding all the useful information pertaining to the extraction of data from
 * the result of the Lucene search query.
 */
class LuceneSearchQueryExtractContext {

	private final ProjectionHitMapper<?, ?> projectionHitMapper;
	private final FromDocumentFieldValueConvertContext convertContext;
	private final IndexSearcher indexSearcher;
	private final Query luceneQuery;
	private final LuceneCollectors luceneCollectors;

	LuceneSearchQueryExtractContext(BackendSessionContext sessionContext,
			ProjectionHitMapper<?, ?> projectionHitMapper,
			IndexSearcher indexSearcher, Query luceneQuery,
			LuceneCollectors luceneCollectors) {
		this.projectionHitMapper = projectionHitMapper;
		this.convertContext = new FromDocumentFieldValueConvertContextImpl( sessionContext );
		this.indexSearcher = indexSearcher;
		this.luceneQuery = luceneQuery;
		this.luceneCollectors = luceneCollectors;
	}

	ProjectionHitMapper<?, ?> getProjectionHitMapper() {
		return projectionHitMapper;
	}

	TopDocs getTopDocs() {
		return luceneCollectors.getTopDocs();
	}

	SearchProjectionExtractContext createProjectionExtractContext() {
		return new SearchProjectionExtractContext(
				indexSearcher, luceneQuery,
				luceneCollectors.getTopDocsCollectors()
		);
	}

	SearchProjectionTransformContext createProjectionTransformContext() {
		return new SearchProjectionTransformContext( convertContext );
	}

	AggregationExtractContext createAggregationExtractContext() {
		return new AggregationExtractContext(
				indexSearcher.getIndexReader(),
				convertContext,
				luceneCollectors.getAllMatchingDocsCollectors()
		);
	}
}
