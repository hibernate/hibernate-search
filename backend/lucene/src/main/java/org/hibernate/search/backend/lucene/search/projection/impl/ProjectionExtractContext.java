/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIdReader;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class ProjectionExtractContext {

	private final TopDocsDataCollectorExecutionContext collectorExecutionContext;
	private final ProjectionHitMapper<?> projectionHitMapper;
	private final LuceneIdReader idReader;

	public ProjectionExtractContext(TopDocsDataCollectorExecutionContext collectorExecutionContext,
			ProjectionHitMapper<?> projectionHitMapper, LuceneIdReader idReader) {
		this.collectorExecutionContext = collectorExecutionContext;
		this.projectionHitMapper = projectionHitMapper;
		this.idReader = idReader;
	}

	public TopDocsDataCollectorExecutionContext collectorExecutionContext() {
		return collectorExecutionContext;
	}

	public ProjectionHitMapper<?> projectionHitMapper() {
		return projectionHitMapper;
	}

	public LuceneIdReader idReader() {
		return idReader;
	}
}
