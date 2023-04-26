/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

public class ProjectionExtractContext {

	private final TopDocsDataCollectorExecutionContext collectorExecutionContext;
	private final ProjectionHitMapper<?> projectionHitMapper;

	public ProjectionExtractContext(TopDocsDataCollectorExecutionContext collectorExecutionContext,
			ProjectionHitMapper<?> projectionHitMapper) {
		this.collectorExecutionContext = collectorExecutionContext;
		this.projectionHitMapper = projectionHitMapper;
	}

	public TopDocsDataCollectorExecutionContext collectorExecutionContext() {
		return collectorExecutionContext;
	}

	public ProjectionHitMapper<?> projectionHitMapper() {
		return projectionHitMapper;
	}
}
