/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
